package ch.heig.dma.nearnote.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="note")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val text: String,
    val locationName: String,
    val latitude: Double,   // TODO : Peut être utile
    val longitude: Double,  // TODO : Peut être utile
    val radius: Float = 100f,  // Rayon en mètres pour le géofencing
    val isActive: Boolean = true
)