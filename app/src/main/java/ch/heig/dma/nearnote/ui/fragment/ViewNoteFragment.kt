package ch.heig.dma.nearnote.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.databinding.FragmentViewNoteBinding
import ch.heig.dma.nearnote.models.Note
import ch.heig.dma.nearnote.utils.viewBinding

/**
 * A DialogFragment for displaying the details of a selected note in a read-only format.
 * Allows the user to close the view or proceed to edit the note.
 */
class ViewNoteFragment : DialogFragment() {

    // ViewBinding delegate for accessing views.
    private val binding by viewBinding(FragmentViewNoteBinding::bind)

    // The note object to display.
    private var noteToView: Note? = null

    companion object {
        private const val ARG_NOTE_TO_VIEW = "ch.heig.dma.nearnote.ARG_NOTE_TO_VIEW"

        /**
         * Factory method to create a new instance of ViewNoteFragment.
         * @param note The [Note] to be displayed.
         * @return A new instance of ViewNoteFragment.
         */
        fun newInstance(note: Note): ViewNoteFragment {
            return ViewNoteFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTE_TO_VIEW, note)
                }
            }
        }
    }

    @Suppress("DEPRECATION") // For getParcelable on older SDKs.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            noteToView = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_NOTE_TO_VIEW, Note::class.java)
            } else {
                it.getParcelable(ARG_NOTE_TO_VIEW)
            }
        }

        // If noteToView is somehow null (should not happen if newInstance is used), dismiss to prevent errors.
        if (noteToView == null) {
            // Use allowStateLoss if there's a chance this is called after onSaveInstanceState.
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_view_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateUI()
        setupClickListeners()
    }

    /** Populates the UI elements with the data from the [noteToView]. */
    private fun populateUI() {
        noteToView?.let { note ->
            binding.tvViewDialogTitle.text = getString(R.string.view_note_title)
            binding.tvViewNoteTitle.text = note.title
            binding.tvViewNoteText.text = note.text

            // Display location information only if a location name is present.
            if (note.locationName.isNotBlank()) {
                binding.tvViewNoteLocation.text = note.locationName
                binding.layoutViewNoteLocation.visibility = View.VISIBLE
            } else {
                binding.layoutViewNoteLocation.visibility = View.GONE
            }
        }
    }

    /** Sets up click listeners for the "Close" and "Edit" buttons. */
    private fun setupClickListeners() {
        binding.btnViewNoteClose.setOnClickListener {
            dismiss()
        }

        binding.btnViewNoteEdit.setOnClickListener {
            noteToView?.let { note ->
                dismiss()
                val editFragment = AddNoteFragment.newInstance(note) // isViewOnly defaults to false
                editFragment.show(parentFragmentManager, "edit_note_from_view_${note.id}")
            }
        }
    }

    /** Sets the dialog window layout parameters to match parent width and wrap content height. */
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}