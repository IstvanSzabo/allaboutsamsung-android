package de.maxisma.allaboutsamsung

import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.experimental.TimeoutCancellationException
import retrofit2.HttpException
import java.io.IOException

abstract class BaseFragment<out InteractionListener : Any> : Fragment() {
    @Suppress("UNCHECKED_CAST")
    protected val listener: InteractionListener
        get() = activity as InteractionListener

    /**
     * If the Exception type is supported, show a notification to the user, otherwise rethrow.
     */
    protected fun displaySupportedError(e: Exception) {
        val message = when(e) {
            is HttpException, is JsonDataException, is JsonEncodingException -> R.string.server_error
            is IOException, is TimeoutCancellationException -> R.string.network_error
            else -> throw e
        }
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }
}