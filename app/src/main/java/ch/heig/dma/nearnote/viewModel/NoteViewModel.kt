package ch.heig.dma.nearnote.viewModel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.heig.dma.nearnote.database.NoteDatabase
import ch.heig.dma.nearnote.geofencing.GeofenceHelper
import ch.heig.dma.nearnote.models.Note
import ch.heig.dma.nearnote.repositories.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Note data and interactions with the GeofenceHelper.
 * Provides LiveData for UI observation and methods for CRUD operations on notes.
 * @param application The application context, used for database and GeofenceHelper initialization.
 */
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    private val geofenceHelper = GeofenceHelper(application) // Helper for managing geofences

    /** LiveData holding the list of all notes. */
    val notes: LiveData<List<Note>>

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        notes = repository.notes
    }

    // Holds the currently selected LatLng pair for a new/edited note, or null if none.
    private val _selectedLocation = MutableLiveData<Pair<Double, Double>?>()

    /** LiveData for the currently selected geographical coordinates (Latitude, Longitude). */
    val selectedLocation: LiveData<Pair<Double, Double>?> = _selectedLocation

    // Holds the display name for the selected location (e.g., POI name, address).
    private val _selectedLocationName = MutableLiveData<String?>()

    /** LiveData for the name associated with the selected location. */
    val selectedLocationName: LiveData<String?> = _selectedLocationName

    /**
     * Sets or updates the selected location.
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @param name Optional display name for the location; null to clear/indicate no specific name.
     */
    fun selectLocation(latitude: Double, longitude: Double, name: String? = null) {
        _selectedLocation.value = latitude to longitude
        _selectedLocationName.value = name
    }

    /** Clears any currently selected location and its associated name. */
    fun clearSelectedLocation() {
        _selectedLocation.value = null
        _selectedLocationName.value = null
    }

    /**
     * Inserts a new note into the database and registers its geofence if applicable.
     * @param note The note to insert.
     */
    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
        // If the new note is active and has a location, add its geofence.
        if (note.isActive && (note.latitude != 0.0 || note.longitude != 0.0)) {
            geofenceHelper.addGeofencesForNotes(listOf(note)) // Add geofence for the new active note
        }
    }

    /**
     * Updates an existing note in the database and updates its geofence registration.
     * @param note The note to update.
     */
    fun update(note: Note) = viewModelScope.launch {
        // Always remove the existing geofence for this note ID to handle all changes (location, active status).
        geofenceHelper.removeGeofencesByIds(listOf(note.id.toString())) // Remove existing

        // If the updated note is active and has a location, re-add its geofence.
        if (note.isActive && (note.latitude != 0.0 || note.longitude != 0.0)) {
            geofenceHelper.addGeofencesForNotes(listOf(note)) // Re-add if still active with location
        }
        repository.update(note)
    }

    /**
     * Deletes a note from the database and removes its associated geofence.
     * @param note The note to delete.
     */
    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
        geofenceHelper.removeGeofencesByIds(listOf(note.id.toString()))
    }

    /**
     * Registers geofences for all currently active notes with locations.
     * Typically called on app start after permissions are granted.
     * This function ensures the geofence state is synced with the database.
     */
    @SuppressLint("MissingPermission") // Permissions are checked in MainActivity before calling
    fun registerAllActiveGeofences() {
        // Use IO dispatcher for database access
        viewModelScope.launch(Dispatchers.IO) {
            val activeNotes = repository.getActiveNotesForGeofencing()
            // Clear all existing geofences first to ensure a clean state.
            geofenceHelper.removeAllGeofences()
            if (activeNotes.isNotEmpty()) {
                geofenceHelper.addGeofencesForNotes(activeNotes)
            }
        }
    }
}