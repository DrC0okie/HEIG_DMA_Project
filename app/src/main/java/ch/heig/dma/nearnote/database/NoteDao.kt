package ch.heig.dma.nearnote.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ch.heig.dma.nearnote.models.Note

/**
 * Data Access Object  for the [Note] entity.
 * Defines methods for interacting with the 'note' table in the database.
 */
@Dao
interface NoteDao {

    /**
     * Retrieves all notes from the table, ordered by ID in descending order (newest first).
     * @return A [LiveData] list of all [Note]s.
     */
    @Query("SELECT * FROM note ORDER BY id DESC")
    fun getAllNotes(): LiveData<List<Note>>

    /**
     * Inserts a new note into the table.
     * @param note The [Note] to insert.
     * @return The row ID of the newly inserted note.
     */
    @Insert
    suspend fun insert(note: Note): Long


    /**
     * Updates an existing note in the table.
     * The note is identified by its primary key (id).
     * @param note The [Note] with updated values.
     */
    @Update
    suspend fun update(note: Note)

    /**
     * Deletes a note from the table.
     * @param note The [Note] to delete.
     */
    @Delete
    suspend fun delete(note: Note)

    /**
     * Retrieves a single note from the table by its ID.
     * @param noteId The ID of the note to retrieve.
     * @return The [Note] object if found, otherwise null.
     */
    @Query("SELECT * FROM note WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    /**
     * Retrieves all notes that are marked as active and have valid latitude/longitude coordinates.
     * This is typically used for fetching notes eligible for geofencing.
     * @return A list of [Note] objects.
     */
    @Query("SELECT * FROM note WHERE isActive = 1 AND (latitude != 0.0 OR longitude != 0.0)")
    suspend fun getActiveNotesForGeofencing(): List<Note>
}