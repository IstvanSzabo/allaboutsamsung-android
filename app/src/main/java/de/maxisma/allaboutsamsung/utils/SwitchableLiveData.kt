package de.maxisma.allaboutsamsung.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * [LiveData] that can be backed by a delegate that can be exchanged.
 * When it is exchanged, observers are notified.
 */
class SwitchableLiveData<T>(delegate: LiveData<T>) : LiveData<T>() {
    var delegate = delegate
        set(value) {
            GlobalScope.launch(Dispatchers.Main) {
                observer?.let {
                    field.removeObserver(it)
                    value.observeForever(it)
                }
                field = value
            }
        }

    private var observer: Observer<T>? = null

    override fun onInactive() {
        super.onInactive()
        GlobalScope.launch(Dispatchers.Main) {
            observer?.let { delegate.removeObserver(it) }
            observer = null
        }
    }

    override fun onActive() {
        super.onActive()

        GlobalScope.launch(Dispatchers.Main) {
            val observer = Observer<T> { value = it }
            delegate.observeForever(observer)
            this@SwitchableLiveData.observer = observer
            value = delegate.value
        }
    }
}