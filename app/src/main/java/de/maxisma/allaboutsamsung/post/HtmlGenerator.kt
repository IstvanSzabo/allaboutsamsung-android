package de.maxisma.allaboutsamsung.post

import de.maxisma.allaboutsamsung.db.Post
import org.intellij.lang.annotations.Language
import java.text.DateFormat
import java.util.TimeZone

private const val BODY_MARGIN = "8px"

// TODO Style wp-caption?
private const val CSS = """
    body {
        margin: $BODY_MARGIN;
    }
    h1, h2, h3, h4, h5, h6, p {
        font-family: 'Roboto', sans-serif;
        font-weight: 300;
    }
    p {
        color: rgb(70, 70, 70);
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
    .meta {

    }
    """

// TODO Get "von" with i18n

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