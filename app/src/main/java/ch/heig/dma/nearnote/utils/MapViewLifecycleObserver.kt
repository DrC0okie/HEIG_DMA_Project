package ch.heig.dma.nearnote.utils

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.MapView

class MapViewLifecycleObserver(
    private val mapView: MapView,
    private val savedInstanceState: Bundle?
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        mapView.onCreate(savedInstanceState)
    }
    override fun onStart(owner: LifecycleOwner)  = mapView.onStart()
    override fun onResume(owner: LifecycleOwner) = mapView.onResume()
    override fun onPause(owner: LifecycleOwner)  = mapView.onPause()
    override fun onStop(owner: LifecycleOwner)   = mapView.onStop()
    override fun onDestroy(owner: LifecycleOwner)= mapView.onDestroy()
}
