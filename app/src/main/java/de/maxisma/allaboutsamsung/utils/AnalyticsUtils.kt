package de.maxisma.allaboutsamsung.utils

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.post.html.generateLandingAnalyticsHtml
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

@SuppressLint("SetJavaScriptEnabled")
fun trackLandingLoad(context: Context) {
    launch(UI) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(
                BuildConfig.WEBVIEW_BASE_URL,
                generateLandingAnalyticsHtml(),
                "text/html",
                Charsets.UTF_8.name(),
                null
            )
            delay(10_000)
            destroy()
        }
    }
}