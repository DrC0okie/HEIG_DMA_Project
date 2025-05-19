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

class NoteAdapter : ListAdapter<Note, NoteAdapter.ViewHolder>(NoteDiffCallback()) {

    // Callbacks pour les actions
    var onNoteClickListener: ((Note) -> Unit)? = null
    var onNoteDeleteListener: ((Note) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvText: TextView = view.findViewById(R.id.tvText)
        private val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

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

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}