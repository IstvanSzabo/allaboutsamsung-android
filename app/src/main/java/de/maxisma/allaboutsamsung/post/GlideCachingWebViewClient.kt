package de.maxisma.allaboutsamsung.post

import android.os.Build
import android.support.annotation.RequiresApi
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.util.concurrent.ExecutionException

class GlideCachingWebViewClient : WebViewClient() {
    private fun shouldInterceptRequestInternal(view: WebView, url: String): WebResourceResponse? {
        val file = try {
            Glide.with(view)
                .asFile()
                .load(url)
                .apply(RequestOptions().onlyRetrieveFromCache(true))
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
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse =
        shouldInterceptRequestInternal(view, url) ?: super.shouldInterceptRequest(view, url)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse =
        shouldInterceptRequestInternal(view, request.url.toString()) ?: super.shouldInterceptRequest(view, request)

}