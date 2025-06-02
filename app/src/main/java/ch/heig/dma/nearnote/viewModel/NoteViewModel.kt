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

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    private val geofenceHelper = GeofenceHelper(application)
    val notes: LiveData<List<Note>>

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        notes = repository.notes
    }

    private val _selectedLocation = MutableLiveData<Pair<Double, Double>?>()
    val selectedLocation: LiveData<Pair<Double, Double>?> = _selectedLocation

    private val _selectedLocationName = MutableLiveData<String?>()
    val selectedLocationName: LiveData<String?> = _selectedLocationName

    /** @param name optional display name; null → clear name */
    fun selectLocation(latitude: Double, longitude: Double, name: String? = null) {
        _selectedLocation.value = latitude to longitude
        _selectedLocationName.value = name
    }

    fun clearSelectedLocation() {
        _selectedLocation.value = null
        _selectedLocationName.value = null
    }

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
        if (note.isActive && (note.latitude != 0.0 || note.longitude != 0.0)) {
            geofenceHelper.addGeofencesForNotes(listOf(note)) // Add geofence for the new active note
        }
    }

    fun update(note: Note) = viewModelScope.launch {
        geofenceHelper.removeGeofencesByIds(listOf(note.id.toString())) // Remove existing
        if (note.isActive && (note.latitude != 0.0 || note.longitude != 0.0)) {
            geofenceHelper.addGeofencesForNotes(listOf(note)) // Re-add if still active with location
        }
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
        geofenceHelper.removeGeofencesByIds(listOf(note.id.toString()))
    }

    // Call this when the app starts or when permissions are granted
    @SuppressLint("MissingPermission")
    fun registerAllActiveGeofences() {
        // This needs direct list access, or observe once
        viewModelScope.launch(Dispatchers.IO) {
            val activeNotes = repository.getActiveNotesForGeofencing()
            if (activeNotes.isNotEmpty()) {
                // It's safer to remove all existing ones first to avoid duplicates or orphaned geofences
                geofenceHelper.removeAllGeofences()
                geofenceHelper.addGeofencesForNotes(activeNotes)
            } else {
                geofenceHelper.removeAllGeofences() // No active notes, ensure no geofences are registered
            }
        }
    }
}