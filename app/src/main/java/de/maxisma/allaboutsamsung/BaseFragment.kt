package de.maxisma.allaboutsamsung

import android.support.v4.app.Fragment

abstract class BaseFragment<out InteractionListener: Any>: Fragment() {
    @Suppress("UNCHECKED_CAST")
    protected val listener: InteractionListener = activity as InteractionListener
}