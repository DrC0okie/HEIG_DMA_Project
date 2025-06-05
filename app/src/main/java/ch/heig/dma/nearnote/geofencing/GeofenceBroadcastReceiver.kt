package ch.heig.dma.nearnote.geofencing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import ch.heig.dma.nearnote.MainActivity
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.database.NoteDatabase
import ch.heig.dma.nearnote.repositories.NoteRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that listens for geofence transition events.
 * When a geofence is triggered, it fetches the corresponding note and posts a notification.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()

    // Scope for database operations, which should be on an IO dispatcher.
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val CHANNEL_ID = "NearNoteGeofenceChannel"

        // Offset for notification IDs to avoid conflicts if note IDs are small or 0.
        private const val NOTIFICATION_ID_OFFSET = 1000
    }

    /**
     * Called when a geofence transition event occurs.
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive triggered with intent action: ${intent.action}")
        Log.d(TAG, "Received Intent extras: ${bundleToString(intent.extras)}")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null, cannot process geofence trigger.")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "GeofencingEvent error: $errorMessage (Code: ${geofencingEvent.errorCode})")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.d(TAG, "Geofence transition: $geofenceTransition")

        // Handle only enter or dwell transitions for notifications.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            Log.d(
                TAG,
                "Triggering geofences: ${triggeringGeofences?.joinToString { it.requestId }}"
            )

            triggeringGeofences?.forEach { geofence ->
                val noteIdString = geofence.requestId
                val noteId = noteIdString.toLongOrNull() // Note ID was stored as string.
                if (noteId != null) {
                    scope.launch { // Launch a coroutine to fetch note details from the database.
                        val noteDao = NoteDatabase.getDatabase(context.applicationContext).noteDao()
                        val repository = NoteRepository(noteDao)
                        val note = repository.getNoteById(noteId)
                        // Send notification only if note exists and is still active.
                        if (note != null && note.isActive) {
                            sendNotification(
                                context,
                                "Near ${note.locationName}",
                                note.title,
                                noteId
                            )
                        } else {
                            Log.w(TAG, "Note not found or inactive for ID: $noteId")
                        }
                    }
                } else {
                    Log.e(TAG, "Invalid requestId (not a Long): $noteIdString")
                }
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(
                TAG,
                "Exited geofence(s): ${geofencingEvent.triggeringGeofences?.joinToString { it.requestId }}"
            )
            // Future: handle exit events (e.g., dismiss notification).
        } else {
            Log.e(TAG, "Invalid geofence transition type: $geofenceTransition")
        }
    }

    /**
     * Creates and displays a notification for a triggered geofence.
     * @param context The context.
     * @param title The title for the notification.
     * @param message The main text content for the notification.
     * @param originalNoteId The actual ID (Long) of the note, used for the tap intent.
     */
    private fun sendNotification(
        context: Context,
        title: String,
        message: String,
        originalNoteId: Long
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android API 26 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "NearNote Geofences"
            val descriptionText = "Notifications for when you are near a note's location"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to launch MainActivity when notification is tapped
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW // Define an action for better intent identification.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Pass the note id
            putExtra(MainActivity.EXTRA_NOTE_ID_FROM_GEOFENCE, originalNoteId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Use originalNoteId as the requestCode for the PendingIntent for uniqueness per note
        val activityPendingIntent =
            PendingIntent.getActivity(
                context,
                originalNoteId.toInt(),
                mainActivityIntent,
                pendingIntentFlags
            )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismiss notification when tapped
            .setContentIntent(activityPendingIntent) // Set the intent that will fire when the user taps the notification
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Use a unique ID for each notification if multiple can appear,
        notificationManager.notify(originalNoteId.toInt() + NOTIFICATION_ID_OFFSET, builder.build())
        Log.i(TAG, "Notification sent for note ID related to: $originalNoteId - '$title: $message'")
    }

    /**
     * Helper function to convert a Bundle to a readable string for logging.
     * @param bundle The bundle to convert.
     * @return A string representation of the bundle's contents.
     */
    private fun bundleToString(bundle: Bundle?): String {
        if (bundle == null) return "null"
        val stringBuilder = StringBuilder("Bundle[")
        for (key in bundle.keySet()) {
            stringBuilder.append(" ").append(key).append("=").append(bundle.get(key)).append(";")
        }
        stringBuilder.append(" ]")
        return stringBuilder.toString()
    }
}