package de.maxisma.allaboutsamsung

import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import pl.aprilapps.easyphotopicker.EasyImage
import retrofit2.HttpException
import java.io.IOException

/**
 * @see listener
 * @see displaySupportedError
 */
abstract class BaseFragment<out InteractionListener : Any> : Fragment() {

    /**
     * Convenience method for casting [getActivity] to [InteractionListener] unsafely.
     */
    @Suppress("UNCHECKED_CAST")
    protected val listener: InteractionListener
        get() = activity as InteractionListener

    /**
     * If the Exception type is supported, show a notification to the user, otherwise rethrow.
     */
    protected fun displaySupportedError(e: Exception) {
        val message = when (e) {
            is HttpException, is JsonDataException, is JsonEncodingException -> R.string.server_error
            is IOException, is TimeoutCancellationException -> R.string.network_error
            else -> throw e
        }
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    private val uiJobs = mutableListOf<Job>()

    /**
     * Launch [f] on the UI thread and cancel it automatically in [onPause]
     */
    protected fun uiLaunch(f: suspend CoroutineScope.() -> Unit): Job {
        val job = launch(UI) { f() }

        synchronized(uiJobs) {
            uiJobs += job
        }
        job.invokeOnCompletion {
            synchronized(uiJobs) {
                uiJobs -= job
            }
        }

        return launch { job.join() }
    }

    override fun onPause() {
        val cause = CancellationException("onPause")
        synchronized(uiJobs) {
            uiJobs.forEach { it.cancel(cause) }
            uiJobs.clear()
        }

        super.onPause()
    }

    var easyImageCallback: EasyImage.Callbacks? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        easyImageCallback?.let {
            // Don't throw Exception here, we could receive this after activity death
            // and won't have a callback. The callback can't be stored easily across
            // activity death and restore.
            EasyImage.handleActivityResult(requestCode, resultCode, data, activity, it)
        }
    }

}