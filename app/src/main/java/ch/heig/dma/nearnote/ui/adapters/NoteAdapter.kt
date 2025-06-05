package ch.heig.dma.nearnote.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.models.Note
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

/**
 * A [ListAdapter] for displaying [Note] items in a RecyclerView.
 * Handles item clicks and delete actions via callbacks.
 */
class NoteAdapter : ListAdapter<Note, NoteAdapter.ViewHolder>(NoteDiffCallback()) {

    /** Callback invoked when a note item is clicked. */
    var onNoteClickListener: ((Note) -> Unit)? = null

    /** Callback invoked when the delete button on a note item is clicked. */
    var onNoteDeleteListener: ((Note) -> Unit)? = null

    /**
     * Creates new [ViewHolder]s (invoked by the layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return ViewHolder(view)
    }

    /**
     * Replaces the contents of a [ViewHolder] (invoked by the layout manager).
     * Binds the data from the [Note] at the given position to the ViewHolder's views.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for a [Note] item.
     * Holds references to the views within the item layout and binds [Note] data to them.
     * @param view The root view of the item_note.xml layout.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvText: TextView = view.findViewById(R.id.tvText)
        private val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        /**
         * Binds a [Note] object to the views in this ViewHolder.
         * Sets up click listeners for the item view and delete button.
         * @param note The [Note] data to display.
         */
        fun bind(note: Note) {
            tvTitle.text = note.title
            tvText.text = note.text
            tvLocation.text = "Lieu: ${note.locationName}"

            itemView.setOnClickListener {
                onNoteClickListener?.invoke(note)
            }

            btnDelete.setOnClickListener {
                onNoteDeleteListener?.invoke(note)
            }
        }
    }

    /**
     * DiffUtil.ItemCallback for calculating the difference between two [Note] lists.
     * Used by ListAdapter to efficiently update the RecyclerView.
     */
    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {

        /** Checks if two items represent the same object (usually by ID). */
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        /** Checks if the content of two items is the same. Called if [areItemsTheSame] is true. */
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}