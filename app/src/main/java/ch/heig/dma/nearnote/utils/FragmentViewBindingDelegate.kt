package ch.heig.dma.nearnote.utils

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A property delegate for Fragments to handle ViewBinding lifecycle.
 * It ensures that the binding is only accessible when the Fragment's view is valid
 * and automatically nullifies the binding when the view is destroyed.
 * @param VB The type of the ViewBinding class.
 * @property viewBindingFactory A lambda function that takes a View and returns an instance of VB.
 */
class FragmentViewBindingDelegate<VB : ViewBinding>(
    private val viewBindingFactory: (View) -> VB
) : ReadOnlyProperty<Fragment, VB>, DefaultLifecycleObserver {

    // Holds the ViewBinding instance. Nullified on view destruction.
    private var binding: VB? = null

    /**
     * Clears the binding reference to prevent memory leaks.
     * @param owner The LifecycleOwner whose state has changed.
     */
    override fun onDestroy(owner: LifecycleOwner) {
        binding = null
    }

    /**
     * Returns the ViewBinding instance.
     * If the binding is null, it creates a new binding using the [viewBindingFactory] and the Fragment's view.
     * It also registers itself as an observer to the Fragment's view lifecycle to clear the binding.
     * @param thisRef The Fragment instance that owns this delegate.
     * @param property The metadata of the property being accessed.
     * @return The ViewBinding instance.
     * @throws IllegalStateException if accessed when the Fragment's view is not available or destroyed.
     */
    override fun getValue(thisRef: Fragment, property: KProperty<*>): VB {
        binding?.let { return it }
        val lifecycle = thisRef.viewLifecycleOwner.lifecycle
        lifecycle.addObserver(this)
        val vb = viewBindingFactory(thisRef.requireView())
        return vb.also { binding = it }
    }
}

/**
 * Extension function to easily create a [FragmentViewBindingDelegate].
 * @param VB The type of the ViewBinding.
 * @param factory A lambda that creates the ViewBinding instance from a View (e.g., `YourBinding::bind`).
 * @return A [FragmentViewBindingDelegate] instance.
 */
fun <VB : ViewBinding> Fragment.viewBinding(factory: (View) -> VB) =
    FragmentViewBindingDelegate(factory)
