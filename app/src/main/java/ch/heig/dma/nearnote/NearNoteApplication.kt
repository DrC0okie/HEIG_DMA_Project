package ch.heig.dma.nearnote

import android.app.Application
import com.google.android.libraries.places.api.Places
import java.util.Locale

class NearNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the SDK
        val apiKey = getString(R.string.google_maps_key)
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey, Locale.getDefault())
        }
    }
}