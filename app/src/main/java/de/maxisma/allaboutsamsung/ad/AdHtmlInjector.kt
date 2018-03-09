package de.maxisma.allaboutsamsung.ad

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.rest.appApi
import de.maxisma.allaboutsamsung.utils.retry
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import retrofit2.HttpException
import java.io.IOException

/**
 * HTML id of the inserted ad
 */
const val AD_CONTAINER_ID = "injected-ad-container"

/**
 * Inject [adHtml] into the [Post.content] and return it.
 */
fun Post.contentWithAd(adHtml: String): String {
    val doc = Jsoup.parse(content)
    val thirdParagraph = doc.getElementsByTag("p").getOrNull(3) ?: return content

    val adContainer = Element("div").apply {
        attr("id", AD_CONTAINER_ID)
        html(adHtml)
    }
    thirdParagraph.before(adContainer)
    return doc.outerHtml()
}

/**
 * Fetch the current ad HTML from the server and save it in the [KeyValueStore].
 */
fun KeyValueStore.updateAdHtml() = launch {
    try {
        retry(
            HttpException::class,
            IOException::class,
            JsonDataException::class,
            JsonEncodingException::class,
            TimeoutCancellationException::class
        ) {
            withTimeout(10_000) {
                adHtml = appApi.adForPost().await().html
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}