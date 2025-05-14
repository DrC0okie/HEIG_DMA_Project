package ch.heig.dma.nearnote.database

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Room
import ch.heig.dma.nearnote.models.Note

@Database(entities = [Note::class], version = 1, exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao() : NoteDao

    companion object {
        @Volatile
        private var INSTANCE : NoteDatabase? = null

        fun getDatabase(context: Context) : NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val _instance = Room.databaseBuilder(context.applicationContext,
                    NoteDatabase::class.java, "note.db")
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = _instance
                _instance
            }
        }
    }
}