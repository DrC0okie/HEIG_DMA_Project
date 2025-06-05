package ch.heig.dma.nearnote

import android.app.Application
import com.google.android.libraries.places.api.Places
import java.util.Locale

/**
 * Custom Application class for NearNote.
 * Initializes global services like the Places SDK.
 */
class NearNoteApplication : Application() {

    /**
     * Called when the application is starting, before any other
     * activity, service, or receiver objects (excluding content providers) have been created.
     */
    override fun onCreate() {
        super.onCreate()

        // Initialize the Google Places SDK if it hasn't been initialized yet.
        val apiKey = getString(R.string.google_maps_key)
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey, Locale.getDefault())
        }
    }
}