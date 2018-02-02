package de.maxisma.allaboutsamsung.rest

import android.webkit.WebView
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.post.html.HtmlTheme
import de.maxisma.allaboutsamsung.post.html.commentsCss
import de.maxisma.allaboutsamsung.utils.await
import de.maxisma.allaboutsamsung.utils.retry
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import retrofit2.HttpException
import java.io.IOException

private const val TIMEOUT_MS = 30_000L

private fun commentsUrl(postId: PostId) = BuildConfig.COMMENTS_URL_TEMPLATE.replace("[POST_ID]", postId.toString())

private fun OkHttpClient.retriedDownloadWithTimeout(commentsUrl: String) = async {
    retry(
        HttpException::class,
        JsonDataException::class,
        JsonEncodingException::class,
        IOException::class,
        TimeoutCancellationException::class
    ) {
        withTimeout(TIMEOUT_MS) {
            newCall(Request.Builder().url(commentsUrl).build()).await()
        }
    }
}

private fun injectCss(html: String, css: String): String {
    val doc = Jsoup.parse(html)
    val style = Element("style").apply {
        attr("type", "text/css")
        text(css)
    }
    doc.head().insertChildren(0, style)
    return doc.outerHtml()
}

fun WebView.loadCommentsFor(postId: PostId, httpClient: OkHttpClient, theme: HtmlTheme, onError: (Exception) -> Unit) {
    launch(UI) {
        val commentsUrl = commentsUrl(postId)
        val html = httpClient.retriedDownloadWithTimeout(commentsUrl).await().body()!!.string()
        val injectedHtml = injectCss(html, theme.commentsCss())
        loadDataWithBaseURL(
            commentsUrl,
            injectedHtml,
            "text/html",
            Charsets.UTF_8.name(),
            null
        )
    }
}