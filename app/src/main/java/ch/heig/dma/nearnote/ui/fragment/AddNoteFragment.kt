package ch.heig.dma.nearnote.ui.fragment

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.databinding.FragmentAddNoteBinding
import ch.heig.dma.nearnote.viewModel.NoteViewModel
import ch.heig.dma.nearnote.models.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class AddNoteFragment : DialogFragment() {

    private var _binding: FragmentAddNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NoteViewModel
    private var editingNote: Note? = null
    private var geocodingInProgress = false

    companion object {
        fun newInstance(note: Note? = null): AddNoteFragment {
            val fragment = AddNoteFragment()
            fragment.editingNote = note
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[NoteViewModel::class.java]

        setupInitialUI()
        setupObservers()
        setupClickListeners()
    }

    private fun setupInitialUI() {
        editingNote?.let { note ->
            binding.tvDialogTitle.text = getString(R.string.modify_note_title)
            binding.etTitle.setText(note.title)
            binding.etText.setText(note.text)

            if (note.latitude != 0.0 || note.longitude != 0.0) {
                viewModel.setSelectedLocationWithName(note.latitude, note.longitude, note.locationName)
            } else {
                viewModel.clearSelectedLocation()
            }
        } ?: run { // New note
            binding.tvDialogTitle.text = getString(R.string.add_note_title)
            viewModel.clearSelectedLocation()
        }
    }

    private fun setupObservers() {
        viewModel.selectedLocationName.observe(viewLifecycleOwner) { displayNameFromViewModel ->
            val currentSelectedCoordinates = viewModel.selectedLocation.value

            if (currentSelectedCoordinates != null) { // A location (lat/lng) is actually selected
                binding.btnClearLocation.visibility = View.VISIBLE
                if (displayNameFromViewModel != null) {
                    // A display name was provided
                    binding.tvLocationDisplay.setText(displayNameFromViewModel)
                } else {
                    // No display name provided from ViewModel, we should geocode the currentSelectedCoordinates.
                    reverseGeocode(currentSelectedCoordinates.first, currentSelectedCoordinates.second)
                }
            } else {
                // No location (lat/lng) is selected, so clear the location name field.
                binding.tvLocationDisplay.setText("")
                binding.btnClearLocation.visibility = View.GONE
            }
        }
    }

    private fun reverseGeocode(latitude: Double, longitude: Double) {
        if (geocodingInProgress) return
        geocodingInProgress = true

        lifecycleScope.launch {
            try {
                val addressText = fetchAddress(latitude, longitude)
                binding.tvLocationDisplay.setText(addressText)
                viewModel.selectedLocation.value?.let { coords ->
                    viewModel.setSelectedLocationWithName(coords.first, coords.second, addressText)
                }
            } catch (e: Exception) {
                Log.e("AddNoteFragment", "Error fetching address: ${e.message}", e)
                binding.tvLocationDisplay.setText(getString(R.string.geocoding_error))
            } finally {
                geocodingInProgress = false
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchAddress(latitude: Double, longitude: Double): String {
        // Ensure context is available for Geocoder and getString
        val currentContext = context ?: return "Context not available for geocoding"
        val geocoder = Geocoder(currentContext, Locale.getDefault())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
            @Suppress("DEPRECATION")
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

    private fun getFallbackLocationString(latitude: Double, longitude: Double): String {
        return "Near ${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)}"
    }

    private fun setupClickListeners() {
        binding.btnSelectLocation.setOnClickListener {
            val mapFragment = MapFragment()
            mapFragment.show(parentFragmentManager, MapFragment.TAG)
        }

        binding.btnClearLocation.setOnClickListener {
            viewModel.clearSelectedLocation()
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val text = binding.etText.text.toString().trim()
            val locationName = binding.tvLocationDisplay.text.toString().trim()
            val currentSelectedLocPair = viewModel.selectedLocation.value

            if (title.isEmpty()) {
                Toast.makeText(context, getString(R.string.write_all_field), Toast.LENGTH_SHORT).show()
                binding.etTitle.error = getString(R.string.write_all_field)
                return@setOnClickListener
            }

            val finalLatitude: Double
            val finalLongitude: Double
            val finalLocationName: String

            if (currentSelectedLocPair != null) {
                finalLatitude = currentSelectedLocPair.first
                finalLongitude = currentSelectedLocPair.second
                finalLocationName = locationName
            } else {
                finalLatitude = 0.0
                finalLongitude = 0.0
                finalLocationName = ""
            }

            val noteToSave = editingNote?.copy(
                title = title, text = text, locationName = finalLocationName,
                latitude = finalLatitude, longitude = finalLongitude
            ) ?: Note(
                title = title, text = text, locationName = finalLocationName,
                latitude = finalLatitude, longitude = finalLongitude
            )

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

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        viewModel.clearSelectedLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}