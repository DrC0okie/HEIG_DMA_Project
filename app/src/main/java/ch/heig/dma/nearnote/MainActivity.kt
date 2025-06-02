package ch.heig.dma.nearnote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import ch.heig.dma.nearnote.models.Note
import ch.heig.dma.nearnote.ui.adapters.NoteAdapter
import ch.heig.dma.nearnote.viewModel.NoteViewModel
import ch.heig.dma.nearnote.ui.fragment.AddNoteFragment
import ch.heig.dma.nearnote.ui.fragment.ViewNoteFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter
    private val requestCode = 123

    companion object {
        const val EXTRA_NOTE_ID_FROM_GEOFENCE = "ch.heig.dma.nearnote.NOTE_ID_FROM_GEOFENCE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = NoteAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        handleGeofenceIntent(intent)
        checkAndRequestPermissions()

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleGeofenceIntent(intent)
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

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        Log.d("MainActivity", "Checking permissions...")

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Requesting ACCESS_FINE_LOCATION")
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            Log.d("MainActivity", "ACCESS_FINE_LOCATION already granted.")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting ACCESS_BACKGROUND_LOCATION")
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                Log.d("MainActivity", "ACCESS_BACKGROUND_LOCATION already granted.")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS")
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("MainActivity", "POST_NOTIFICATIONS already granted.")
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d("MainActivity", "Calling ActivityCompat.requestPermissions for: ${permissionsToRequest.joinToString()}")
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), requestCode)
        } else {
            Log.d("MainActivity", "All required permissions already granted. Registering geofences.")
            viewModel.registerAllActiveGeofences()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (this.requestCode == requestCode) {
            Log.d("MainActivity", "onRequestPermissionsResult received for requestCode: $requestCode")
            // Check if FINE_LOCATION was granted, as it's the absolute minimum
            val fineLocationGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (fineLocationGranted) {
                Log.d("MainActivity", "ACCESS_FINE_LOCATION granted. Registering geofences.")
                viewModel.registerAllActiveGeofences()

                // Now check for background location (if applicable) separately for user guidance
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val backgroundLocationGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (!backgroundLocationGranted) {
                        Log.w("MainActivity", "ACCESS_BACKGROUND_LOCATION was not granted.")
                        // If rationale should be shown, it means user just denied it normally.
                        // If rationale should NOT be shown, it might be "Deny & Don't Ask Again" or policy.
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            // User selected "Deny & Don't Ask Again" or policy prevents asking.
                            // Guide them to settings.
                            showPermissionDeniedDialog(
                                "Background location is needed for geofence alerts when the app is closed. Please enable it in app settings.",
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        } else {
                            // User denied it but didn't say "don't ask again". You could show a simpler Toast.
                            Toast.makeText(this, "Background location enhances geofencing. You can grant it later.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                // Similarly check for POST_NOTIFICATIONS if needed for guidance
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationsGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    if (!notificationsGranted && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                        showPermissionDeniedDialog(
                            "Notification permission is needed to show geofence alerts. Please enable it in app settings.",
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else if (!notificationsGranted) {
                        Toast.makeText(this, "Notifications allow timely geofence alerts.", Toast.LENGTH_LONG).show()
                    }
                }

            } else {
                Log.e("MainActivity", "ACCESS_FINE_LOCATION was not granted. Essential for geofencing.")
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // User selected "Deny & Don't Ask Again" for fine location.
                    showPermissionDeniedDialog(
                        "Precise location is essential for this app's geofencing features to work. Please enable it in app settings.",
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    // User denied fine location, but can be asked again.
                    Toast.makeText(this, "Location permission is essential for geofencing.", Toast.LENGTH_LONG).show()
                }
                // Do NOT register geofences if fine location is not granted.
            }
        }
    }

    private fun showPermissionDeniedDialog(message: String, permission: String) {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("App Settings") { _, _ ->
                // Intent to open app settings
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleGeofenceIntent(intent: Intent?) {
        intent?.let { currentIntent ->
            if (currentIntent.hasExtra(EXTRA_NOTE_ID_FROM_GEOFENCE)) {
                val noteId = currentIntent.getLongExtra(EXTRA_NOTE_ID_FROM_GEOFENCE, -1L)
                if (noteId != -1L) {
                    Log.d("MainActivity", "Received geofence tap for note ID: $noteId")

                    val noteObserver = object : androidx.lifecycle.Observer<List<Note>> {
                        override fun onChanged(notes: List<Note>) {
                            val targetNote = notes.find { note -> note.id == noteId }
                            if (targetNote != null) {
                                // We found the note, remove the observer to prevent multiple dialogs
                                viewModel.notes.removeObserver(this)

                                if (!isFinishing && !isDestroyed) {
                                    val viewFragment = ViewNoteFragment.newInstance(targetNote) // Create ViewNoteFragment
                                    viewFragment.show(supportFragmentManager, "view_note_from_geofence_${noteId}")
                                }
                            } else {
                                if (notes.isNotEmpty()) {
                                    Toast.makeText(this@MainActivity, "Note (ID: $noteId) not found.", Toast.LENGTH_SHORT).show()
                                    viewModel.notes.removeObserver(this)
                                }
                            }
                        }
                    }
                    viewModel.notes.observe(this, noteObserver)
                    currentIntent.removeExtra(EXTRA_NOTE_ID_FROM_GEOFENCE)
                }
            }
        }
    }
}