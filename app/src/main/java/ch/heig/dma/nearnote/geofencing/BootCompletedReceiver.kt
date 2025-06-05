package ch.heig.dma.nearnote.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ch.heig.dma.nearnote.database.NoteDatabase
import ch.heig.dma.nearnote.repositories.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that listens for the device boot completion event.
 * Re-registers all active geofences when the device starts up.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()

    // Scope for database operations.
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "BootReceiver"
    }

    /**
     * Called when the BroadcastReceiver is receiving an Intent broadcast.
     * Checks for ACTION_BOOT_COMPLETED to re-register geofences.
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device boot completed. Re-registering geofences.")

            scope.launch {
                val noteDao = NoteDatabase.getDatabase(context.applicationContext).noteDao()
                val repository = NoteRepository(noteDao)

                // Fetch all notes that should have active geofences.
                val activeNotes = repository.getActiveNotesForGeofencing()
                if (activeNotes.isNotEmpty()) {

                    // Ensure no stray geofences are active if no notes should be geofenced.
                    GeofenceHelper(context).addGeofencesForNotes(activeNotes)
                } else {
                    Log.i(TAG, "No active notes to register geofences for on boot.")
                }
            }
        }
    }
}