package ch.heig.dma.nearnote.repositories

import androidx.lifecycle.LiveData
import ch.heig.dma.nearnote.database.NoteDao
import ch.heig.dma.nearnote.models.Note

class NoteRepository(private val noteDao: NoteDao) {
    val notes: LiveData<List<Note>> = noteDao.getAllNotes()

    suspend fun insert(note: Note): Long {
        return noteDao.insert(note)
    }

    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    suspend fun getNoteById(noteId: Long): Note? {
        return noteDao.getNoteById(noteId)
    }
}