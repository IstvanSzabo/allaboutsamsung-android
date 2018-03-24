package de.maxisma.allaboutsamsung.utils

import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.os.toUri
import de.maxisma.allaboutsamsung.BaseFragment
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import java.io.File

/**
 * A [WebChromeClient] with extra features.
 *
 * @param progressBar A [ProgressBar] to be kept updated while loading
 * @param supportWindowCreation If true, webpages are allowed to open [WebViewDialog]
 */
open class ExtendedWebChromeClient(
    private val progressBar: ProgressBar?,
    private val supportWindowCreation: Boolean = false,
    private val fragment: BaseFragment<*>
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

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback
        fragment.easyImageCallback = object : DefaultCallback() {
            override fun onImagePicked(imageFile: File?, source: EasyImage.ImageSource?, type: Int) {
                this@ExtendedWebChromeClient.filePathCallback?.onReceiveValue(imageFile?.let { arrayOf(it.toUri()) })
                this@ExtendedWebChromeClient.filePathCallback = null
            }
        }
        EasyImage.openChooserWithGallery(fragment, null, 0)
        return true
    }
}