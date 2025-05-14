package ch.heig.dma.nearnote.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ch.heig.dma.nearnote.R
import ch.heig.dma.nearnote.models.Note

class NoteAdapter : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

    var notes: List<Note> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // Callbacks pour les actions
    var onNoteClickListener: ((Note) -> Unit)? = null
    var onNoteDeleteListener: ((Note) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = notes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notes[position])
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
}