package ch.heig.dma.nearnote.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName="note")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val text: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Float = 100f,  // Rayon en mètres pour le géofencing
    val isActive: Boolean = true
): Parcelable {
    /** Return a copy with updated text/title/location. */
    fun updated(
        newTitle: String,
        newText: String,
        loc: Location?
    ) = copy(
        title = newTitle,
        text = newText,
        locationName = loc?.name.orEmpty(),
        latitude = loc?.lat ?: 0.0,
        longitude = loc?.lng ?: 0.0
    )
}