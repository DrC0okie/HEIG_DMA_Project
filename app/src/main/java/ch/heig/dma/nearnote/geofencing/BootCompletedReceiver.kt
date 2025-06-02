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

class BootCompletedReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device boot completed. Re-registering geofences.")

            scope.launch {
                val noteDao = NoteDatabase.getDatabase(context.applicationContext).noteDao()
                val repository = NoteRepository(noteDao)
                val activeNotes = repository.getActiveNotesForGeofencing()
                if (activeNotes.isNotEmpty()) {
                    GeofenceHelper(context).addGeofencesForNotes(activeNotes)
                } else {
                    Log.i(TAG, "No active notes to register geofences for on boot.")
                }
            }
        }
    }
}