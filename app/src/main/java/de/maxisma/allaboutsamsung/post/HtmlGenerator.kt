package de.maxisma.allaboutsamsung.post

import android.content.Context
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Post
import java.text.DateFormat
import java.util.TimeZone

private const val BODY_MARGIN = "8px"
private const val LIGHT_TEXT_COLOR = "rgb(70, 70, 70)"
private const val DEFAULT_FONT_CONFIG = """
font-family: 'Roboto', sans-serif;
font-weight: 300;
"""

private const val CSS = """
    body {
        margin: $BODY_MARGIN;
        $DEFAULT_FONT_CONFIG
    }
    h1, h2, h3, h4, h5, h6 {
        $DEFAULT_FONT_CONFIG
    }
    p {
        color: $LIGHT_TEXT_COLOR;
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
        color: $LIGHT_TEXT_COLOR;
    }
    """

private val dateFormatter = (DateFormat.getDateInstance().clone() as DateFormat).apply {
    timeZone = TimeZone.getDefault()
}

abstract class PostHtmlGenerator {
    protected abstract fun formatAuthorName(authorName: String): String

    fun generateHtml(post: Post, authorName: String) = """<html>
<head>
    <title>${post.title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
    <style type="text/css">$CSS</style>
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