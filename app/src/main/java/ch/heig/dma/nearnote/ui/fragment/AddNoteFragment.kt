package ch.heig.dma.nearnote.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.viewModel.NoteViewModel
import ch.heig.dma.nearnote.models.Note

class AddNoteFragment : DialogFragment() {

    private lateinit var viewModel: NoteViewModel
    private var editingNote: Note? = null

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
        return inflater.inflate(R.layout.fragment_add_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(NoteViewModel::class.java)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etText = view.findViewById<EditText>(R.id.etText)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val btnSelectLocation = view.findViewById<Button>(R.id.btnSelectLocation)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val tvDialogTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        tvDialogTitle.text = if (editingNote != null) getString(R.string.modify_note_title) else getString(
            R.string.add_note_title
        )

        // Si mode édition => pré-remplir les champs
        editingNote?.let { note ->
            tvDialogTitle.text = "Modifier la note"
            etTitle.setText(note.title)
            etText.setText(note.text)
            etLocation.setText(note.locationName)
            viewModel.setSelectedLocation(note.latitude, note.longitude)
            btnSelectLocation.text = "Localisation sélectionnée"
        } ?: run {
            tvDialogTitle.text = "Ajouter une note"
        }

        // Observer pour la localisation sélectionnée
        viewModel.selectedLocation.observe(viewLifecycleOwner) { location ->
            // maj de l'ui quand une localisation est sélectionnée
            if (location != null) {
                btnSelectLocation.text = "Localisation sélectionnée"
            }
        }

//        btnSelectLocation.setOnClickListener {
//            val mapFragment = MapFragment()
//            mapFragment.show(parentFragmentManager, "MAP")
//        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val text = etText.text.toString().trim()
            val locationName = etLocation.text.toString().trim()
            val location = viewModel.selectedLocation.value

            if (title.isEmpty() || locationName.isEmpty()) {
                Toast.makeText(context, getString(R.string.write_all_field), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO : A voir si vrmnt on veut garder ou pas
//            if (location == null) {
//                Toast.makeText(context, "Veuillez sélectionner un emplacement sur la carte", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }

            val note = editingNote?.copy(
                title = title,
                text = text,
                locationName = locationName,
                latitude = 1.0, // pr la compilation
                longitude = 2.0 // pr la compilation
//                latitude = location.first,
//                longitude = location.second
            ) ?: Note(
                title = title,
                text = text,
                locationName = locationName,
                latitude = 1.0, // pr la compilation
                longitude = 2.0 // pr la compilation
//                latitude = location.first,
//                longitude = location.second
            )

            if (editingNote != null) {
                viewModel.update(note)
            } else {
                viewModel.insert(note)
            }

            viewModel.clearSelectedLocation()
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}