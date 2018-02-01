package de.maxisma.allaboutsamsung.post.html

import android.content.Context
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Post
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

    fun generateHtml(post: Post, authorName: String, theme: HtmlTheme) = """<html>
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
</body>
</html>
"""
}

class AndroidPostHtmlGenerator(private val context: Context) : PostHtmlGenerator() {
    override fun formatAuthorName(authorName: String): String = context.getString(R.string.author_template, authorName)
}