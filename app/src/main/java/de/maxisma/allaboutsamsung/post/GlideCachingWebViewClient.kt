package de.maxisma.allaboutsamsung.post

import android.os.Build
import android.support.annotation.RequiresApi
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import de.maxisma.allaboutsamsung.utils.glide.GlideApp
import java.util.concurrent.ExecutionException

/**
 * A [WebViewClient] backed by Glide. Tries to retrieve images from its cache,
 * otherwise falls back to the normal implementation.
 */
open class GlideCachingWebViewClient : WebViewClient() {
    private fun shouldInterceptRequestInternal(view: WebView, url: String): WebResourceResponse? {
        val file = try {
            GlideApp.with(view)
                .asFile()
                .load(url)
                .onlyRetrieveFromCache(true)
                .submit()
                .get()
        } catch (e: ExecutionException) {
            return null
        }
        return WebResourceResponse(
            "image/*",
            Charsets.UTF_8.name(),
            file.inputStream()
        )
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
        shouldInterceptRequestInternal(view, url) ?: super.shouldInterceptRequest(view, url)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        shouldInterceptRequestInternal(view, request.url.toString()) ?: super.shouldInterceptRequest(view, request)

}