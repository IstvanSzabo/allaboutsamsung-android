package de.maxisma.allaboutsamsung.utils

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.post.html.generateLandingAnalyticsHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Run JS that tracks a hit on the main page
 */
@SuppressLint("SetJavaScriptEnabled")
fun trackLandingLoad(context: Context) {
    GlobalScope.launch(Dispatchers.Main) {
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