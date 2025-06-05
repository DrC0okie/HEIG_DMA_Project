package ch.heig.dma.nearnote.repositories

import androidx.lifecycle.LiveData
import ch.heig.dma.nearnote.database.NoteDao
import ch.heig.dma.nearnote.models.Note

/**
 * Repository class that abstracts access to multiple data sources (in this case, only Room DAO).
 * Provides a clean API for data operations to the ViewModel.
 * @param noteDao The Data Access Object for notes.
 */
class NoteRepository(private val noteDao: NoteDao) {

    /** LiveData stream of all notes, ordered by ID descending. */
    val notes: LiveData<List<Note>> = noteDao.getAllNotes()

    /**
     * Inserts a new note into the database.
     * @param note The [Note] to insert.
     * @return The row ID of the newly inserted note.
     */
    suspend fun insert(note: Note): Long {
        return noteDao.insert(note)
    }

    /**
     * Updates an existing note in the database.
     * @param note The [Note] to update.
     */
    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    /**
     * Deletes a note from the database.
     * @param note The [Note] to delete.
     */
    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    /**
     * Retrieves a specific note by its ID.
     * @param noteId The ID of the note to retrieve.
     * @return The [Note] if found, otherwise null.
     */
    suspend fun getNoteById(noteId: Long): Note? {
        return noteDao.getNoteById(noteId)
    }

    /**
     * Retrieves all active notes that have a valid location, suitable for geofencing.
     * @return A list of [Note] objects.
     */
    suspend fun getActiveNotesForGeofencing(): List<Note> {
        return noteDao.getActiveNotesForGeofencing()
    }
}