package de.maxisma.allaboutsamsung.post

import de.maxisma.allaboutsamsung.db.Post
import org.intellij.lang.annotations.Language

private const val BODY_MARGIN = "8px"

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
    img[class^="align"]:not([class^="attachment"]) {
    	width: calc(100% + 2 * $BODY_MARGIN);
        height: auto;
        margin-left: -$BODY_MARGIN;
        margin-right: -$BODY_MARGIN;
    }
    .shariff {
        display: none;
    }
    """

// TODO Add author

@Language("HTML")
fun Post.toHtml(): String = """<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
    <style type="text/css">$CSS</style>
</head>
<body>
<h1>$title</h1>
$content
</body>
</html>
"""