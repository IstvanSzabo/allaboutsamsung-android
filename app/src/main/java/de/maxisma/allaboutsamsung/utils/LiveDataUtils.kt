package de.maxisma.allaboutsamsung.utils

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations

inline fun <T> LiveData<T>.observe(owner: LifecycleOwner, crossinline observer: (T?) -> Unit) =
    observe(owner, Observer { observer(it) })

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