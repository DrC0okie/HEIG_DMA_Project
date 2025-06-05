package ch.heig.dma.nearnote.geofencing

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import ch.heig.dma.nearnote.models.Note
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Helper class to manage geofence registration and removal.
 * Encapsulates interactions with the FusedLocationProviderClient's GeofencingClient.
 * @param context The application or activity context.
 */
class GeofenceHelper(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        private const val TAG = "GeofenceHelper"

        // Geofences will not expire automatically.
        private const val GEOFENCE_EXPIRATION_MILLISECONDS: Long = Geofence.NEVER_EXPIRE
    }

    // Lazily creates a PendingIntent that will be delivered to GeofenceBroadcastReceiver.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * Adds geofences for a list of notes.
     * Only active notes with valid locations will have geofences created.
     * Removes all existing geofences if the provided list is empty or results in no geofences.
     * Requires location permissions to be granted.
     * @param notes The list of notes to potentially create geofences for.
     */
    @SuppressLint("MissingPermission") // Permissions are expected to be checked by the caller.
    fun addGeofencesForNotes(notes: List<Note>) {
        if (notes.isEmpty()) {
            Log.d(TAG, "No notes provided to add geofences for.")
            removeAllGeofences() // Clear existing if no new ones are to be added.
            return
        }

        if (!hasRequiredPermissions()) {
            Log.w(TAG, "Cannot add geofences, missing permissions.")
            return
        }

        val geofenceList = mutableListOf<Geofence>()
        for (note in notes) {
            // Only create geofences for active notes that have a valid location.
            if (note.isActive && (note.latitude != 0.0 || note.longitude != 0.0)) {
                geofenceList.add(
                    Geofence.Builder()
                        .setRequestId(note.id.toString()) // Note ID serves as the unique geofence identifier.
                        .setCircularRegion(
                            note.latitude,
                            note.longitude,
                            note.radius
                        )
                        .setExpirationDuration(GEOFENCE_EXPIRATION_MILLISECONDS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build()
                )
            }
        }

        if (geofenceList.isEmpty()) {
            Log.d(TAG, "No active notes with locations found to create geofences.")
            removeAllGeofences() // Clear existing if no valid geofences were built.
            return
        }

        val geofencingRequest = GeofencingRequest.Builder().apply {
            // Trigger if device is already inside a geofence.
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.i(TAG, "Successfully added ${geofenceList.size} geofences.")
            }
            addOnFailureListener { exception ->
                Log.e(TAG, "Failed to add geofences: ${exception.message}", exception)
                // Handle errors, e.g., GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE
            }
        }
    }

    /**
     * Removes specific geofences identified by their request IDs (note IDs).
     * @param noteIds A list of note IDs (as Strings) for which to remove geofences.
     */
    @SuppressLint("MissingPermission") // Permission check mainly for consistency, removal might work without.
    fun removeGeofencesByIds(noteIds: List<String>) {
        if (noteIds.isEmpty()) return

        // While removal might not strictly need permissions, logging if they are missing.
        if (!hasRequiredPermissions()) {
            Log.w(TAG, "Cannot remove geofences, permissions might be an issue for re-adding.")
        }

        geofencingClient.removeGeofences(noteIds).run {
            addOnSuccessListener {
                Log.i(TAG, "Successfully removed geofences: $noteIds")
            }
            addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove geofences $noteIds: ${exception.message}", exception)
            }
        }
    }

    /**
     * Removes all geofences previously registered with the app's [geofencePendingIntent].
     */
    @SuppressLint("MissingPermission")
    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.i(TAG, "Successfully removed all geofences associated with the PendingIntent.")
            }
            addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove all geofences: ${exception.message}", exception)
            }
        }
    }

    /**
     * Checks if the app has the necessary location permissions for geofencing.
     * @return True if all required permissions are granted, false otherwise.
     */
    private fun hasRequiredPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        // Background location permission is required for geofencing to work when app is not in foreground.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
}