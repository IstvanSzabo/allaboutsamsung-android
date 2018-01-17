package de.maxisma.allaboutsamsung.utils

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer

inline fun <T> LiveData<T>.observe(owner: LifecycleOwner, crossinline observer: (T?) -> Unit) =
    observe(owner, Observer { observer(it) })