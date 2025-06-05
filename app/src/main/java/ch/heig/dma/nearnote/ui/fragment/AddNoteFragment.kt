package ch.heig.dma.nearnote.ui.fragment

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.databinding.FragmentAddNoteBinding
import ch.heig.dma.nearnote.models.Location
import ch.heig.dma.nearnote.viewModel.NoteViewModel
import ch.heig.dma.nearnote.models.Note
import ch.heig.dma.nearnote.utils.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * A DialogFragment for adding a new note or editing an existing one.
 * Allows users to input title, text, and select a location via a map.
 */
class AddNoteFragment : DialogFragment(R.layout.fragment_add_note) {

    // ViewBinding delegate for accessing views.
    private val binding by viewBinding(FragmentAddNoteBinding::bind)

    private lateinit var viewModel: NoteViewModel
    private var editingNote: Note? = null

    // Coroutine job for the current geocoding operation.
    private var geocodeJob: Job? = null

    companion object {
        // Key for passing a Note object as an argument.
        private const val ARG_NOTE = "ARG_NOTE"

        /**
         * Factory method to create a new instance of this fragment.
         * @param note The note to edit, or null to create a new note.
         * @return A new instance of AddNoteFragment.
         */
        fun newInstance(note: Note?): AddNoteFragment =
            AddNoteFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTE, note)
                }
            }
    }

    @Suppress("DEPRECATION") // For getParcelable on older SDKs.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingNote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_NOTE, Note::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable<Note>(ARG_NOTE)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[NoteViewModel::class.java]

        setupInitialUI()
        setupObservers()
        setupClickListeners()
    }

    /** Populates UI fields based on whether a note is being edited or a new one is being added. */
    private fun setupInitialUI() {
        editingNote?.let { note -> // Editing existing note
            binding.tvDialogTitle.text = getString(R.string.modify_note_title)
            binding.etTitle.setText(note.title)
            binding.etText.setText(note.text)
            // tvLocationDisplay will be populated by the observer via ViewModel.

            if (note.latitude != 0.0 || note.longitude != 0.0) {
                viewModel.selectLocation(note.latitude, note.longitude, note.locationName)
            } else {
                viewModel.clearSelectedLocation()
            }
        } ?: run { // New note
            binding.tvDialogTitle.text = getString(R.string.add_note_title)
            viewModel.clearSelectedLocation() // Ensures location fields start empty/default.
        }
    }

    /** Sets up observers for LiveData from the ViewModel to update the UI. */
    private fun setupObservers() {
        // Observes the selected location name to update the location display text.
        viewModel.selectedLocationName.observe(viewLifecycleOwner) { displayNameFromViewModel ->
            val currentSelectedCoordinates = viewModel.selectedLocation.value

            if (currentSelectedCoordinates != null) { // A location (lat/lng) is actually selected
                binding.btnClearLocation.visibility = View.VISIBLE
                if (displayNameFromViewModel != null) {
                    // A display name was provided
                    binding.tvLocationDisplay.text = displayNameFromViewModel
                } else {
                    // No display name provided from ViewModel, we should geocode the currentSelectedCoordinates.
                    reverseGeocode(currentSelectedCoordinates.first, currentSelectedCoordinates.second)
                }
            } else {
                // No location (lat/lng) is selected, so clear the location name field.
                binding.tvLocationDisplay.text = ""
                binding.btnClearLocation.visibility = View.GONE
            }
        }
    }

    /**
     * Performs reverse geocoding for the given coordinates and updates the UI and ViewModel.
     * Cancels any ongoing geocode operation before starting a new one.
     * @param latitude The latitude to geocode.
     * @param longitude The longitude to geocode.
     */
    private fun reverseGeocode(latitude: Double, longitude: Double) {
        geocodeJob?.cancel() // Cancel any previous geocoding job.
        geocodeJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val addressText = fetchAddress(latitude, longitude)
                binding.tvLocationDisplay.text = addressText
                viewModel.selectedLocation.value?.let { coords ->
                    viewModel.selectLocation(coords.first, coords.second, addressText)
                }
            } catch (e: Exception) {
                Log.e("AddNoteFragment", "Error fetching address: ${e.message}", e)
                binding.tvLocationDisplay.text = getString(R.string.geocoding_error)
            }
        }
    }

    /**
     * Fetches a human-readable address string for the given coordinates using Geocoder.
     * Handles platform differences for Geocoder API.
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @return A formatted address string, a fallback string, or an error message.
     */
    @OptIn(ExperimentalCoroutinesApi::class) // For suspendCancellableCoroutine
    private suspend fun fetchAddress(latitude: Double, longitude: Double): String {
        // Ensure context is available for Geocoder and getString
        val currentContext = context ?: return "Context not available for geocoding"
        val geocoder = Geocoder(currentContext, Locale.getDefault())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude, longitude, 1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                if (addresses.isNotEmpty()) {
                                    continuation.resume(formatAddress(addresses[0], latitude, longitude), null)
                                } else {
                                    Log.w("AddNoteFragment", "No address found (API 33+)")
                                    continuation.resume(getFallbackLocationString(latitude, longitude), null)
                                }
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                Log.e("AddNoteFragment", "Geocoder error (API 33+): $errorMessage")
                                // Ensure context for getString in error case
                                val errorMsg =
                                    if (isAdded && context != null) getString(R.string.geocoding_error) else "Geocoding error"
                                continuation.resume(errorMsg, null)
                            }
                        }
                    }
                )
                continuation.invokeOnCancellation {
                    Log.d("AddNoteFragment", "Geocoding coroutine (API 33+) cancelled.")
                }
            }
        } else {
            @Suppress("DEPRECATION") // Pre-Android 13
            withContext(Dispatchers.IO) {
                try {
                    val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        formatAddress(addresses[0], latitude, longitude)
                    } else {
                        Log.w("AddNoteFragment", "No address found (pre-API 33)")
                        getFallbackLocationString(latitude, longitude)
                    }
                } catch (e: IOException) {
                    Log.e("AddNoteFragment", "Geocoder IOException (pre-API 33)", e)
                    if (isAdded) getString(R.string.geocoding_error) else "Geocoding error"
                } catch (e: IllegalArgumentException) {
                    Log.e("AddNoteFragment", "Geocoder IllegalArgumentException (pre-API 33)", e)
                    "Invalid coordinates"
                }
            }
        }
    }

    /**
     * Formats an [Address] object into a displayable string.
     * @param address The [Address] object.
     * @param latitude Fallback latitude if address parts are empty.
     * @param longitude Fallback longitude if address parts are empty.
     * @return A formatted address string or a fallback "Near lat,lon" string.
     */
    private fun formatAddress(address: Address, latitude: Double, longitude: Double): String {
        val addressFragments = listOfNotNull(
            address.featureName,
            address.thoroughfare,
            address.locality,
            address.adminArea,
            address.countryName
        ).filter { it.isNotBlank() }

        return if (addressFragments.isNotEmpty()) {
            addressFragments.joinToString(", ")
        } else {
            getFallbackLocationString(latitude, longitude)
        }
    }

    /**
     * Provides a fallback string indicating proximity to coordinates if geocoding yields no address.
     * @param latitude The latitude.
     * @param longitude The longitude.
     * @return A string like "Near lat, lon".
     */
    private fun getFallbackLocationString(latitude: Double, longitude: Double): String {
        return "Near ${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)}"
    }

    /** Sets up click listeners for the dialog's buttons. */
    private fun setupClickListeners() {
        binding.btnSelectLocation.setOnClickListener {
            val mapFragment = MapFragment()
            mapFragment.show(parentFragmentManager, MapFragment.TAG)
        }

        binding.btnClearLocation.setOnClickListener {
            viewModel.clearSelectedLocation() // Observer will update UI.
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val text = binding.etText.text.toString().trim()

            if (title.isEmpty()) {
                binding.etTitle.error = getString(R.string.write_all_field)
                Toast.makeText(context, getString(R.string.write_all_field), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Build a Location? only if coordinates were selected
            val selectedPair = viewModel.selectedLocation.value
            val location: Location? = selectedPair?.let { (lat, lng) ->
                // Note: tvLocationDisplay already shows the "address" or fallback string
                Location(lat, lng, binding.tvLocationDisplay.text.toString().trim())
            }

            val baseNote = editingNote ?: Note()
            val noteToSave = baseNote.updated(title, text, location)

            // Dispatch to ViewModel
            if (editingNote != null) {
                viewModel.update(noteToSave)
            } else {
                viewModel.insert(noteToSave)
            }

            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    /** Ensures ViewModel's selected location is cleared when the dialog is dismissed. */
    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        viewModel.clearSelectedLocation()
    }

    /** Sets the dialog window layout parameters. */
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}