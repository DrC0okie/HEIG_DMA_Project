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

class MapFragment : DialogFragment(R.layout.fragment_map), OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    private val binding by viewBinding(FragmentMapBinding::bind)

    private lateinit var viewModel: NoteViewModel
    private var googleMap: GoogleMap? = null
    private var currentSelectedMarker: Marker? = null
    private var currentSelectedLatLng: LatLng? = null
    private val defaultLocation = LatLng(47.3769, 8.5417)
    private val defaultZoom = 10f
    private val selectedZoom = 16f
    private var currentSelectedPlaceName: String? = null
    private var currentSelectedPlaceAddress: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycle.addObserver(
            MapViewLifecycleObserver(binding.mapView, savedInstanceState)
        )
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        binding.mapView.getMapAsync(this)
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

    private fun setupAutocompleteFragment() {
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment_container)
                    as AutocompleteSupportFragment

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
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, selectedZoom))
                    // Store both name and address if available
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.setOnPoiClickListener(this)

        // When map is ready, check if ViewModel has a previously selected location
        val previouslySelectedCoords = viewModel.selectedLocation.value
        val previouslySavedDisplayName = viewModel.selectedLocationName.value

        if (previouslySelectedCoords != null) {
            val initialLatLng = LatLng(previouslySelectedCoords.first, previouslySelectedCoords.second)
            addOrUpdateMarker(initialLatLng, previouslySavedDisplayName, null)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, selectedZoom))
        } else {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, defaultZoom))
        }

        googleMap?.setOnMapClickListener { latLng ->
            addOrUpdateMarker(latLng, null, null)
        }
    }

    override fun onPoiClick(poi: PointOfInterest) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, selectedZoom))
        addOrUpdateMarker(poi.latLng, poi.name, null)
    }

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

        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, selectedZoom))
    }

    override fun onDestroyView() {
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