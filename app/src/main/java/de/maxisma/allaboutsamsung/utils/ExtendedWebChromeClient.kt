package de.maxisma.allaboutsamsung.utils

import android.os.Message
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar

/**
 * A [WebChromeClient] with extra features.
 *
 * @param progressBar A [ProgressBar] to be kept updated while loading
 * @param supportWindowCreation If true, webpages are allowed to open [WebViewDialog]
 */
open class ExtendedWebChromeClient(
    private val progressBar: ProgressBar?,
    private val supportWindowCreation: Boolean = false
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progressBar?.apply {
            visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            progress = newProgress
        }
    }
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        if (!supportWindowCreation) return false

        WebViewDialog(view.context) {
            (resultMsg?.obj as? WebView.WebViewTransport)?.webView = it
            resultMsg?.sendToTarget()
        }.show()

        return true
    }
}