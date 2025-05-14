package ch.heig.dma.nearnote.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.heig.dma.nearnote.database.NoteDatabase
import ch.heig.dma.nearnote.models.Note
import ch.heig.dma.nearnote.repositories.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val notes: LiveData<List<Note>>

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        notes = repository.notes
    }

    private val _selectedLocation = MutableLiveData<Pair<Double, Double>?>()
    val selectedLocation: LiveData<Pair<Double, Double>?> = _selectedLocation

    fun setSelectedLocation(latitude: Double, longitude: Double) {
        _selectedLocation.value = Pair(latitude, longitude)
    }

    fun clearSelectedLocation() {
        _selectedLocation.value = null
    }

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }
}