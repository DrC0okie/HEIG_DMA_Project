package ch.heig.dma.nearnote.utils

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FragmentViewBindingDelegate<VB : ViewBinding>(
    private val viewBindingFactory: (View) -> VB
) : ReadOnlyProperty<Fragment, VB>, DefaultLifecycleObserver {

    private var binding: VB? = null

    override fun onDestroy(owner: LifecycleOwner) {
        binding = null
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): VB {
        binding?.let { return it }
        val lifecycle = thisRef.viewLifecycleOwner.lifecycle
        lifecycle.addObserver(this)
        val vb = viewBindingFactory(thisRef.requireView())
        return vb.also { binding = it }
    }
}

fun <VB : ViewBinding> Fragment.viewBinding(factory: (View) -> VB) =
    FragmentViewBindingDelegate(factory)
