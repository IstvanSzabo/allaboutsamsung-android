package de.maxisma.allaboutsamsung.post.html

import android.content.Context
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Post
import okhttp3.HttpUrl
import java.text.DateFormat
import java.util.TimeZone
import android.support.annotation.ColorInt as ColorIntAnnotation

private val dateFormatter = (DateFormat.getDateInstance().clone() as DateFormat).apply {
    timeZone = TimeZone.getDefault()
}

abstract class PostHtmlGenerator {
    protected abstract fun formatAuthorName(authorName: String): String

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

    fun generateHtml(post: Post, authorName: String, theme: HtmlTheme, analyticsJs: String) = """<html>
<head>
    <title>${post.title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
    <style type="text/css">${theme.postCss()}</style>
</head>
<body>
<h1>${post.title}</h1>
<span class="meta">${formatAuthorName(authorName)}, ${dateFormatter.format(post.dateUtc)}</span>
${post.content}

<script type="text/javascript">
$analyticsJs
</script>
</body>
</html>
"""
}

class AndroidPostHtmlGenerator(private val context: Context) : PostHtmlGenerator() {
    override fun formatAuthorName(authorName: String): String = context.getString(R.string.author_template, authorName)
}

private fun generateAnalyticsJs(accountId: String, relativeUrl: String) = """
var _gaq = _gaq || [];
_gaq.push(['_setAccount', '$accountId']);
_gaq.push(['_gat._anonymizeIp']);
_gaq.push(['_setCustomVar', 1, 'MobileApp', 'Android-${BuildConfig.VERSION_NAME}']);
_gaq.push(['_trackPageview', '$relativeUrl']);
(function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();
"""

fun Post.generateAnalyticsJs() = generateAnalyticsJs(
    accountId = BuildConfig.GOOGLE_ANALYTICS_ID,
    relativeUrl = HttpUrl.parse(link)!!.encodedPath()
)

private fun generateLandingAnalyticsJs() = generateAnalyticsJs(BuildConfig.GOOGLE_ANALYTICS_ID, "/")
fun generateLandingAnalyticsHtml() = """<html>
<head>
<script type="text/javascript">
${generateLandingAnalyticsJs()}
</script>
</head>
</html>
"""