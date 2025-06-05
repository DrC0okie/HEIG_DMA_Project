package ch.heig.dma.nearnote.database

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Room
import ch.heig.dma.nearnote.models.Note

/**
 * The Room database for the application.
 * Contains the `note` table and provides access to its DAO.
 */
@Database(entities = [Note::class], version = 1, exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao() : NoteDao

    companion object {
        @Volatile
        private var INSTANCE : NoteDatabase? = null

        /**
         * Gets the singleton instance of [NoteDatabase].
         * Creates the database if it doesn't exist yet.
         *
         * @param context The application context.
         * @return The singleton [NoteDatabase] instance.
         */
        fun getDatabase(context: Context) : NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext,
                    NoteDatabase::class.java, "note.db")
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance // Return the created instance.
            }
        }
    }
}