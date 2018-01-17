package de.maxisma.allaboutsamsung.post

import de.maxisma.allaboutsamsung.db.Post
import org.intellij.lang.annotations.Language
import java.text.DateFormat
import java.util.TimeZone

private const val BODY_MARGIN = "8px"
private const val LIGHT_TEXT_COLOR = "rgb(70, 70, 70)"
private const val DEFAULT_FONT_CONFIG = """
font-family: 'Roboto', sans-serif;
font-weight: 300;
"""

// TODO Style wp-caption?
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
    img[class^="align"]:not([class^="attachment"]), iframe {
    	width: calc(100% + 2 * $BODY_MARGIN);
        max-width: none; /* Override max-width from img rule */
        height: auto;
        margin-left: -$BODY_MARGIN;
        margin-right: -$BODY_MARGIN;
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

// TODO Get "von" with i18n
// TODO Deal with collapseomatic (see post.php)

private val dateFormatter = (DateFormat.getDateInstance().clone() as DateFormat).apply {
    timeZone = TimeZone.getDefault()
}

@Language("HTML")
fun Post.toHtml(authorName: String): String = """<html>
<head>
    <title>$title</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
    <style type="text/css">$CSS</style>
</head>
<body>
<h1>$title</h1>
<span class="meta">Von $authorName, ${dateFormatter.format(dateUtc)}</span>
$content
</body>
</html>
"""