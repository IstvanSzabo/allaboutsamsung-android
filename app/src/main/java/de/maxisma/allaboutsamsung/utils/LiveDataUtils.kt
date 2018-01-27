package de.maxisma.allaboutsamsung.utils

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer

inline fun <T> LiveData<T>.observe(owner: LifecycleOwner, crossinline observer: (T?) -> Unit) =
    observe(owner, Observer { observer(it) })

inline fun <T> LiveData<T>.observeUntilFalse(owner: LifecycleOwner, crossinline observer: (T?) -> Boolean) {
    observe(owner, object : Observer<T> {
        override fun onChanged(t: T?) {
            if (!observer(t)) {
                removeObserver(this)
            }
        }
    })
}