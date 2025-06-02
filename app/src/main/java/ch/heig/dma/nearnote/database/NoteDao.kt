package ch.heig.dma.nearnote.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ch.heig.dma.nearnote.models.Note

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY id DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM note WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("SELECT * FROM note WHERE isActive = 1 AND (latitude != 0.0 OR longitude != 0.0)")
    suspend fun getActiveNotesForGeofencing(): List<Note>
}