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

class ViewNoteFragment : DialogFragment() {

    private val binding by viewBinding(FragmentViewNoteBinding::bind)

    private var noteToView: Note? = null

    companion object {
        private const val ARG_NOTE_TO_VIEW = "ch.heig.dma.nearnote.ARG_NOTE_TO_VIEW"

        fun newInstance(note: Note): ViewNoteFragment {
            return ViewNoteFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTE_TO_VIEW, note)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            noteToView = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_NOTE_TO_VIEW, Note::class.java)
            } else {
                it.getParcelable(ARG_NOTE_TO_VIEW)
            }
        }
        if (noteToView == null) {
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

    private fun populateUI() {
        noteToView?.let { note ->
            binding.tvViewDialogTitle.text = getString(R.string.view_note_title)
            binding.tvViewNoteTitle.text = note.title
            binding.tvViewNoteText.text = note.text

            if (note.locationName.isNotBlank()) {
                binding.tvViewNoteLocation.text = note.locationName
                binding.layoutViewNoteLocation.visibility = View.VISIBLE
            } else {
                binding.layoutViewNoteLocation.visibility = View.GONE
            }
        }
    }

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

    override fun onStart() {
        super.onStart()
        // Make dialog fill width and wrap content height
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}