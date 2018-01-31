package de.maxisma.allaboutsamsung.post

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Post
import java.text.DateFormat
import java.util.TimeZone
import android.support.annotation.ColorInt as ColorIntAnnotation

typealias ColorInt = Int

val ColorInt.hexString get() = String.format("#%06X", 0xFFFFFF and this)

data class HtmlTheme(
    @ColorIntAnnotation val lightTextColor: ColorInt,
    @ColorIntAnnotation val backgroundColor: ColorInt,
    @ColorIntAnnotation val defaultTextColor: ColorInt
// TODO Different link color for dark theme
)

fun Context.obtainHtmlThemes() = HtmlThemes(
    lightTheme = HtmlTheme(
        lightTextColor = 0x464646,
        backgroundColor = Color.WHITE,
        defaultTextColor = Color.BLACK
    ),
    darkTheme = HtmlTheme(
        lightTextColor = 0xB4B4B4,
        backgroundColor = TypedValue().let {
            theme.resolveAttribute(R.attr.colorPrimaryDark, it, true)
            it.data
        },
        defaultTextColor = Color.WHITE
    )
)

data class HtmlThemes(
    val lightTheme: HtmlTheme,
    val darkTheme: HtmlTheme
)

private const val BODY_MARGIN = "8px"
private const val DEFAULT_FONT_CONFIG = """
font-family: 'Roboto', sans-serif;
font-weight: 300;
"""

private fun HtmlTheme.css() = """
    body {
        margin: $BODY_MARGIN;
        $DEFAULT_FONT_CONFIG
        background-color: ${backgroundColor.hexString};
        color: ${defaultTextColor.hexString};
    }
    h1, h2, h3, h4, h5, h6 {
        $DEFAULT_FONT_CONFIG
    }
    p {
        color: ${lightTextColor.hexString};
    }
    img {
        max-width: 100%;
        height: auto;
    }
    [class*="align"]:not([class*="attachment"]), iframe {
    	width: calc(100% + 2 * $BODY_MARGIN);
        max-width: none; /* Override max-width from img rule */
        height: auto;
        margin-left: -$BODY_MARGIN;
        margin-right: -$BODY_MARGIN;
    }
    p.wp-caption-text {
        padding: $BODY_MARGIN;
        background-color: lightgray;
        margin: 0 0 16px 0;
    }
    .shariff {
        display: none;
    }
    a:link {
        text-decoration: none;
    }
    h1 {
        margin-bottom: 0.1em;
    }
    .meta {
        margin-bottom: 0.25em;
        display: inline-block;
        color: ${lightTextColor.hexString};
    }
    """

private val dateFormatter = (DateFormat.getDateInstance().clone() as DateFormat).apply {
    timeZone = TimeZone.getDefault()
}

abstract class PostHtmlGenerator {
    protected abstract fun formatAuthorName(authorName: String): String

    fun generateHtml(post: Post, authorName: String, theme: HtmlTheme) = """<html>
<head>
    <title>${post.title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
    <style type="text/css">${theme.css()}</style>
</head>
<body>
<h1>${post.title}</h1>
<span class="meta">${formatAuthorName(authorName)}, ${dateFormatter.format(post.dateUtc)}</span>
${post.content}
</body>
</html>
"""
}

class AndroidPostHtmlGenerator(private val context: Context) : PostHtmlGenerator() {
    override fun formatAuthorName(authorName: String): String = context.getString(R.string.author_template, authorName)
}