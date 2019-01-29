package de.maxisma.allaboutsamsung.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

inline fun <T> LiveData<T>.observe(owner: LifecycleOwner, crossinline observer: (T?) -> Unit) =
    observe(owner, Observer { observer(it) })

/**
 * Observe the data until the observer returns false or the [owner] does not want to receive updates anymore
 */
inline fun <T> LiveData<T>.observeUntilFalse(owner: LifecycleOwner? = null, crossinline observer: (T?) -> Boolean) {
    if (owner != null) {
        observe(owner, object : Observer<T> {
            override fun onChanged(t: T?) {
                if (!observer(t)) {
                    removeObserver(this)
                }
            }
        })
    } else {
        observeForever(object : Observer<T> {
            override fun onChanged(t: T?) {
                if (!observer(t)) {
                    removeObserver(this)
                }
            }
        })
    }
}

inline fun <T, U> LiveData<T>.map(crossinline f: (T) -> U): LiveData<U> = Transformations.map(this) { f(it) }