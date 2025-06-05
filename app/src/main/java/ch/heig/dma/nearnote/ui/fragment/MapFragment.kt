package ch.heig.dma.nearnote.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.databinding.FragmentMapBinding
import ch.heig.dma.nearnote.utils.MapViewLifecycleObserver
import ch.heig.dma.nearnote.utils.viewBinding
import ch.heig.dma.nearnote.viewModel.NoteViewModel
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

/**
 * A DialogFragment that displays a Google Map for location selection.
 * Allows users to pick a location by tapping on the map, a POI, or searching via Autocomplete.
 * The selected location (coordinates and name/address) is passed back via a shared ViewModel.
 */
class MapFragment : DialogFragment(R.layout.fragment_map), OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    private val binding by viewBinding(FragmentMapBinding::bind)

    private lateinit var viewModel: NoteViewModel
    private var googleMap: GoogleMap? = null
    private var currentSelectedMarker: Marker? = null
    private var currentSelectedLatLng: LatLng? = null

    // Default map view settings (Zurich)
    private val defaultLocation = LatLng(47.3769, 8.5417)
    private val defaultZoom = 10f

    // Zoom level for a selected POI or searched place
    private val selectedZoom = 16f

    // Temporary storage for name/address from the current map interaction
    private var currentSelectedPlaceName: String? = null
    private var currentSelectedPlaceAddress: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Initialize MapView lifecycle observer for proper map lifecycle management in a fragment.
        viewLifecycleOwner.lifecycle.addObserver(
            MapViewLifecycleObserver(binding.mapView, savedInstanceState)
        )

        // Set dialog to take up full screen for better map interaction.
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        binding.mapView.getMapAsync(this) // Initialize the map.
        viewModel = ViewModelProvider(requireActivity())[NoteViewModel::class.java]
        setupAutocompleteFragment()

        binding.btnConfirmLocation.setOnClickListener {
            currentSelectedLatLng?.let { latLng ->
                // Construct a display name for the ViewModel.
                val displayNameForViewModel: String? = when {
                    currentSelectedPlaceName != null && currentSelectedPlaceAddress != null ->
                        "$currentSelectedPlaceName, $currentSelectedPlaceAddress"

                    currentSelectedPlaceName != null -> currentSelectedPlaceName
                    // If only address was somehow set (unlikely, but for completeness)
                    currentSelectedPlaceAddress != null -> currentSelectedPlaceAddress
                    else -> null // No name or address from map pick, AddNoteFragment will geocode
                }
                if (displayNameForViewModel != null) {
                    viewModel.selectLocation(
                        latLng.latitude,
                        latLng.longitude,
                        displayNameForViewModel
                    )
                } else {
                    // Raw map click, no name/address from POI/Search.
                    viewModel.selectLocation(latLng.latitude, latLng.longitude)
                }
                dismiss()
            } ?: run {
                Toast.makeText(context, getString(R.string.select_location), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Sets up the AutocompleteSupportFragment for location search. */
    private fun setupAutocompleteFragment() {
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment_container)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(
                    TAG,
                    "Place selected: ${place.name}, ID: ${place.id}, Address: ${place.address}, LatLng: ${place.latLng}"
                )
                place.latLng?.let { latLng ->
                    // To recenter the map on the user selected location
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, selectedZoom))
                    // Store name and address if available
                    addOrUpdateMarker(latLng, place.name, place.address) // Pass address too
                } ?: run {
                    Toast.makeText(context, "Selected place has no location data.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(status: Status) {
                Log.e(TAG, "An error occurred during place selection: $status")
                if (status.statusCode != com.google.android.gms.common.api.CommonStatusCodes.CANCELED) {
                    Toast.makeText(
                        context,
                        "Error selecting place: ${status.statusMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    /**
     * Called when the map is ready to be used.
     * Initializes map settings and loads any previously selected location.
     * @param map The [GoogleMap] object.
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Show zoom controls.
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        // Listen for POI clicks.
        googleMap?.setOnPoiClickListener(this)

        // Initialize map position: use ViewModel's selected location or default.
        val previouslySelectedCoords = viewModel.selectedLocation.value
        val previouslySavedDisplayName = viewModel.selectedLocationName.value

        if (previouslySelectedCoords != null) {
            val initialLatLng = LatLng(previouslySelectedCoords.first, previouslySelectedCoords.second)

            // When loading, use the display name from ViewModel. Address part is implicitly null here.
            addOrUpdateMarker(initialLatLng, previouslySavedDisplayName, null)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, selectedZoom))
        } else {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, defaultZoom))
        }

        // Listener for clicks directly on the map (not on a POI or marker).
        googleMap?.setOnMapClickListener { latLng ->

            // Clears picked name/address.
            addOrUpdateMarker(latLng, null, null)
        }
    }

    /**
     * Called when a Point of Interest on the map is clicked.
     * @param poi The [PointOfInterest] that was clicked.
     */
    override fun onPoiClick(poi: PointOfInterest) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, selectedZoom))

        // POIs provide a name directly; address is not part of the POI object.
        addOrUpdateMarker(poi.latLng, poi.name, null)
    }

    /**
     * Adds or updates a marker on the map for the selected location.
     * Stores the picked name and address locally in the fragment.
     * @param latLng The coordinates of the marker.
     * @param name The primary name of the selected place (e.g., POI name, searched place name).
     * @param address The address of the selected place (if available, typically from Autocomplete).
     */
    private fun addOrUpdateMarker(latLng: LatLng, name: String?, address: String? = null) {
        currentSelectedMarker?.remove()
        val markerTitle = name ?: getString(R.string.location_selected_marker_title)
        currentSelectedMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(markerTitle)
        )
        currentSelectedLatLng = latLng
        currentSelectedPlaceName = name
        currentSelectedPlaceAddress = address

        // Animate camera to the newly selected/marked location.
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, selectedZoom))
    }

    /** Cleans up map-related resources when the fragment's view is destroyed. */
    override fun onDestroyView() {
        // Nullify references to avoid memory leaks. MapView's lifecycle is handled by MapViewLifecycleObserver.
        googleMap = null
        currentSelectedMarker = null
        currentSelectedLatLng = null
        currentSelectedPlaceName = null
        currentSelectedPlaceAddress = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    companion object {
        val TAG: String = MapFragment::class.java.simpleName
    }
}