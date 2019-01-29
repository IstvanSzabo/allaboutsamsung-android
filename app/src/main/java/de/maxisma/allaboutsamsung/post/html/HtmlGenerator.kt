package de.maxisma.allaboutsamsung.post.html

import android.content.Context
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.ad.contentWithAd
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.gallery.findFullImgUrl
import okhttp3.HttpUrl
import org.jsoup.nodes.Document
import java.text.DateFormat
import java.util.TimeZone
import androidx.annotation.ColorInt as ColorIntAnnotation

/**
 * Date formatter for displaying dates to the user
 */
private val dateFormatter = (DateFormat.getDateInstance().clone() as DateFormat).apply {
    timeZone = TimeZone.getDefault()
}

abstract class PostHtmlGenerator {

    /**
     * Format the author name in a user-facing manner
     */
    protected abstract fun formatAuthorName(authorName: String): String

    /**
     * Website to show while loading a post
     */
    fun generateEmptyHtml(theme: HtmlTheme) = """<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
    <style type="text/css">${theme.postCss()}</style>
</head>
<body>
</body>
</html>
"""

    private fun makeImgFiguresClickable(doc: Document) {
        val imgFigures = doc.getElementsByTag("figure")
            .asSequence()
            .filter { it.hasClass("wp-block-image") }
            .toList()
        for (imgFigure in imgFigures) {
            val imgChild = imgFigure.getElementsByTag("img").singleOrNull() ?: continue
            val fullImgUrl = imgChild.findFullImgUrl()

            imgChild.remove()
            val a = doc.createElement("a").apply {
                attr("href", fullImgUrl)
                appendChild(imgChild)
            }

            imgFigure.appendChild(a)
        }
    }

    /**
     * Generate the HTML to be displayed for the [post].
     *
     * @param analyticsJs The JS code to be run for analytics
     * @param adHtml Ad HTML to inject in the post
     */
    fun generateHtml(post: Post, authorName: String, theme: HtmlTheme, analyticsJs: String, adHtml: String) = """<html>
<head>
    <title>${post.title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
    <style type="text/css">${theme.postCss()}</style>
</head>
<body>
<h1>${post.title}</h1>
<span class="meta">${formatAuthorName(authorName)}, ${dateFormatter.format(post.dateUtc)}</span>
${post.contentWithAd(adHtml, ::makeImgFiguresClickable)}

<script type="text/javascript">
$analyticsJs
</script>
<script type="text/javascript">
function resizeIframes() {
    var iframes = document.getElementsByTagName("iframe");
    for (var i = 0; i < iframes.length; i++) {
        var ifr = iframes[i];
        var aspect = ifr.width / ifr.height;
        var newHeight = (1 / aspect) * ifr.clientWidth;
        ifr.style.height = newHeight;
    }
}

window.onload = function() {
    resizeIframes();
}
</script>
</body>
</html>
"""
}

class AndroidPostHtmlGenerator(private val context: Context) : PostHtmlGenerator() {
    override fun formatAuthorName(authorName: String): String = context.getString(R.string.author_template, authorName)
}

/**
 * Generate JS code that reports a visit to [relativeUrl] on the specified account
 */
private fun generateAnalyticsJs(accountId: String, relativeUrl: String) = """
var _gaq = _gaq || [];
_gaq.push(['_setAccount', '$accountId']);
_gaq.push(['_gat._anonymizeIp']);
_gaq.push(['_setCustomVar', 1, 'MobileApp', 'AndroidApp-${BuildConfig.VERSION_NAME}']);
_gaq.push(['_trackPageview', '$relativeUrl']);
(function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();
"""

/**
 * Generate the tracking code for this post
 */
fun Post.generateAnalyticsJs() = generateAnalyticsJs(
    accountId = BuildConfig.GOOGLE_ANALYTICS_ID,
    relativeUrl = HttpUrl.parse(link)!!.encodedPath()
)

/**
 * Generate the tracking code for the "main page"
 */
private fun generateLandingAnalyticsJs() = generateAnalyticsJs(BuildConfig.GOOGLE_ANALYTICS_ID, "/")

fun generateLandingAnalyticsHtml() = """<html>
<head>
<script type="text/javascript">
${generateLandingAnalyticsJs()}
</script>
</head>
</html>
"""