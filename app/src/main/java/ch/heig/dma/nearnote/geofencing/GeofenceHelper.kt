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

class GeofenceHelper(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        private const val TAG = "GeofenceHelper"
        private const val GEOFENCE_EXPIRATION_MILLISECONDS: Long = Geofence.NEVER_EXPIRE
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun addGeofencesForNotes(notes: List<Note>) {
        if (notes.isEmpty()) {
            Log.d(TAG, "No notes provided to add geofences for.")
            removeAllGeofences()
            return
        }

        if (!hasRequiredPermissions()) {
            Log.w(TAG, "Cannot add geofences, missing permissions.")
            return
        }

        val geofenceList = mutableListOf<Geofence>()
        for (note in notes) {
            if (note.isActive && (note.latitude != 0.0 || note.longitude != 0.0)) {
                geofenceList.add(
                    Geofence.Builder()
                        .setRequestId(note.id.toString()) // Use note ID as unique request ID
                        .setCircularRegion(
                            note.latitude,
                            note.longitude,
                            note.radius // Use radius from the note
                        )
                        .setExpirationDuration(GEOFENCE_EXPIRATION_MILLISECONDS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build()
                )
            }
        }

        if (geofenceList.isEmpty()) {
            Log.d(TAG, "No active notes with locations found to create geofences.")
            removeAllGeofences()
            return
        }

        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // Trigger if already inside
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

    @SuppressLint("MissingPermission")
    fun removeGeofencesByIds(noteIds: List<String>) {
        if (noteIds.isEmpty()) return
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

    private fun hasRequiredPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
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