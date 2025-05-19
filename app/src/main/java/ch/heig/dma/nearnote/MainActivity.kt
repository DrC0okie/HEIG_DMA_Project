package ch.heig.dma.nearnote

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import ch.heig.dma.nearnote.models.Note
import ch.heig.dma.nearnote.ui.adapters.NoteAdapter
import ch.heig.dma.nearnote.viewModel.NoteViewModel
import ch.heig.dma.nearnote.ui.fragment.AddNoteFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = NoteAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        adapter.onNoteClickListener = { note ->
            // Ouvrir le fragment d'édition
            val editFragment = AddNoteFragment.newInstance(note)
            editFragment.show(supportFragmentManager, getString(R.string.edit_note))
        }

        adapter.onNoteDeleteListener = { note ->
            // Confirmer la suppression
            showDeleteConfirmationDialog(note)
        }

        viewModel.notes.observe(this) { notes ->
            adapter.submitList(notes)
        }

        findViewById<FloatingActionButton>(R.id.fabAddNote).setOnClickListener {
            showAddNoteDialog()
        }
    }

    private fun showAddNoteDialog() {
        val addNoteFragment = AddNoteFragment()
        addNoteFragment.show(supportFragmentManager, getString(R.string.add_note))
    }

    private fun showDeleteConfirmationDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_note))
            .setMessage(getString(R.string.confirm_delete_note))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.delete(note)
            }
            .setNegativeButton(getString(R.string.abort), null)
            .show()
    }
}