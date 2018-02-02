package de.maxisma.allaboutsamsung.post.html

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import de.maxisma.allaboutsamsung.R

typealias ColorInt = Int

val ColorInt.hexString get() = String.format("#%06X", 0xFFFFFF and this)

data class HtmlTheme(
    @android.support.annotation.ColorInt val lightTextColor: ColorInt,
    @android.support.annotation.ColorInt val backgroundColor: ColorInt,
    @android.support.annotation.ColorInt val defaultTextColor: ColorInt,
    @android.support.annotation.ColorInt val linkColor: ColorInt? = null
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
        defaultTextColor = Color.WHITE,
        linkColor = TypedValue().let {
            theme.resolveAttribute(R.attr.colorAccent, it, true)
            it.data
        }
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

fun HtmlTheme.commentsCss() = """
    *, html body {
        background-color: ${backgroundColor.hexString};
        color: ${defaultTextColor.hexString}
    }
    a:link {
        text-decoration: none;
        color: ${linkColor?.hexString ?: "unset"};
    }
"""

fun HtmlTheme.postCss() = """
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
        color: ${linkColor?.hexString ?: "unset"};
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