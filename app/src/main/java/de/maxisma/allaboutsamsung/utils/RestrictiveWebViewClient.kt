package de.maxisma.allaboutsamsung.utils

import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

open class RestrictiveWebViewClient(private val allowedHosts: Set<String>?) : WebViewClient() {
    protected open fun shouldInterceptUnrestrictedRequest(view: WebView, url: String): WebResourceResponse? = null

    private fun shouldInterceptRequestInternal(view: WebView, url: String) = if (allowedHosts != null && url.toHttpUrlOrNull()?.host !in allowedHosts) {
        null
    } else {
        shouldInterceptUnrestrictedRequest(view, url)
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
        shouldInterceptRequestInternal(view, url) ?: super.shouldInterceptRequest(view, url)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        shouldInterceptRequestInternal(view, request.url.toString()) ?: super.shouldInterceptRequest(view, request)
}