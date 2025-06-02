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
import ch.heig.dma.nearnote.MainActivity // Or your target activity
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.database.NoteDatabase // To access repository
import ch.heig.dma.nearnote.repositories.NoteRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job) // IO for database access

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val CHANNEL_ID = "NearNoteGeofenceChannel"
        private const val NOTIFICATION_ID_OFFSET = 1000 // To avoid collision with other notifications
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive triggered")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "GeofencingEvent error: $errorMessage (Code: ${geofencingEvent.errorCode})")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.d(TAG, "Geofence transition: $geofenceTransition")

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            Log.d(TAG, "Triggering geofences: ${triggeringGeofences?.joinToString { it.requestId }}")

            triggeringGeofences?.forEach { geofence ->
                val noteIdString = geofence.requestId
                val noteId = noteIdString.toLongOrNull()
                if (noteId != null) {
                    scope.launch { // Launch coroutine for DB access
                        val noteDao = NoteDatabase.getDatabase(context.applicationContext).noteDao()
                        val repository = NoteRepository(noteDao)
                        val note = repository.getNoteById(noteId)
                        if (note != null && note.isActive) { // Double check if still active
                            sendNotification(context, "Near ${note.locationName}", note.title, noteId)
                        } else {
                            Log.w(TAG, "Note not found or inactive for ID: $noteId")
                        }
                    }
                } else {
                    Log.e(TAG, "Invalid requestId (not a Long): $noteIdString")
                }
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "Exited geofence(s): ${geofencingEvent.triggeringGeofences?.joinToString { it.requestId }}")
        } else {
            Log.e(TAG, "Invalid geofence transition type: $geofenceTransition")
        }
    }

    private fun sendNotification(context: Context, title: String, message: String, originalNoteId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Pass the original noteId (as Long)
            putExtra(MainActivity.EXTRA_NOTE_ID_FROM_GEOFENCE, originalNoteId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Use originalNoteId as the requestCode for the PendingIntent for uniqueness per note
        val activityPendingIntent =
            PendingIntent.getActivity(context, originalNoteId.toInt(), mainActivityIntent, pendingIntentFlags)

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

    // Call this when the BroadcastReceiver is no longer needed
    fun cancelScope() {
        job.cancel()
    }

    // Helper function to print Bundle contents (add this inside the class or as a top-level fun)
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