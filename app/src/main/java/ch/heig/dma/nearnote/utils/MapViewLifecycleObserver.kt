package ch.heig.dma.nearnote.utils

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.MapView

/**
 * Forwards Fragment lifecycle events to a [MapView].
 * Used for resource management of a MapView when used within a Fragment.
 *
 * @property mapView The [MapView] instance to which lifecycle events will be forwarded.
 * @property savedInstanceState The Bundle from [Fragment.onViewCreated] or [Fragment.onCreate],
 *                              used for [MapView.onCreate].
 */
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
