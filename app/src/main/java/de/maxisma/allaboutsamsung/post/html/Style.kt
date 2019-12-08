package de.maxisma.allaboutsamsung.post.html

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.ad.AD_CONTAINER_ID

typealias ColorInt = Int

val ColorInt.hexString get() = String.format("#%06X", 0xFFFFFF and this)

data class HtmlTheme(
    @androidx.annotation.ColorInt val lightTextColor: ColorInt,
    @androidx.annotation.ColorInt val backgroundColor: ColorInt,
    @androidx.annotation.ColorInt val defaultTextColor: ColorInt,
    @androidx.annotation.ColorInt val adBackgroundColor: ColorInt,
    @androidx.annotation.ColorInt val highlightBackgroundColor: ColorInt,
    @androidx.annotation.ColorInt val highlightForegroundColor: ColorInt,
    @androidx.annotation.ColorInt val linkColor: ColorInt? = null
)

fun Context.obtainHtmlThemes() = HtmlThemes(
    lightTheme = HtmlTheme(
        lightTextColor = 0x464646,
        backgroundColor = retrieveColor(android.R.attr.windowBackground),
        defaultTextColor = Color.BLACK,
        adBackgroundColor = 0xededed,
        highlightBackgroundColor = retrieveColor(R.attr.colorPrimary),
        highlightForegroundColor = Color.WHITE
    ),
    darkTheme = HtmlTheme(
        lightTextColor = 0xB4B4B4,
        backgroundColor = retrieveColor(R.attr.colorPrimaryDark),
        defaultTextColor = Color.WHITE,
        adBackgroundColor = retrieveColor(R.attr.colorPrimaryDark).darken(factor = 0.4f),
        highlightBackgroundColor = retrieveColor(R.attr.colorPrimaryDark).darken(),
        highlightForegroundColor = 0xB4B4B4,
        linkColor = retrieveColor(R.attr.colorAccent)
    )
)

private fun Context.retrieveColor(@AttrRes attr: Int) = TypedValue().let {
    theme.resolveAttribute(attr, it, true)
    it.data
}

private fun ColorInt.darken(factor: Float = 0.6f): ColorInt {
    val hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    hsv[2] *= factor
    return Color.HSVToColor(hsv)
}

data class HtmlThemes(
    val lightTheme: HtmlTheme,
    val darkTheme: HtmlTheme
)

private const val BODY_MARGIN = "8px"
private const val DEFAULT_FONT_CONFIG = """
font-family: 'Roboto', sans-serif;
font-weight: 300;
"""

/**
 * CSS for the comments section
 */
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

/**
 * CSS for the post itself
 */
fun HtmlTheme.postCss() = """
    html {
        max-width: 500px;
        margin: 0 auto;
    }
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
    [class*="align"]:not([class*="attachment"]), iframe, #$AD_CONTAINER_ID, figure.wp-block-image {
    	width: calc(100% + 2 * $BODY_MARGIN);
        max-width: none; /* Override max-width from img rule */
        height: auto;
        margin-left: -$BODY_MARGIN;
        margin-right: -$BODY_MARGIN;
    }
    #$AD_CONTAINER_ID {
        background-color: ${adBackgroundColor.hexString};
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
        ${linkColor?.hexString?.let { "color: $it;" } ?: ""}
    }
    h1 {
        margin-bottom: 0.1em;
    }
    .meta {
        margin-bottom: 0.25em;
        display: inline-block;
        color: ${lightTextColor.hexString};
    }
    blockquote.highlight {
        background-color: ${highlightBackgroundColor.hexString};
        margin-left: -$BODY_MARGIN;
        margin-right: -$BODY_MARGIN;
        padding: 2em;
    }
    blockquote.highlight p {
        color: ${highlightForegroundColor.hexString};
        font-style: normal;
        font-family: inherit;
        font-weight: inherit;
    }
    blockquote em {
        color: ${highlightForegroundColor.hexString};
        font-style: normal;
    }
    .wp-block-gallery {
        display: flex;
        flex-wrap: wrap;
        list-style-type: none;
        padding: 0;
    }

    .wp-block-gallery .blocks-gallery-image, .wp-block-gallery .blocks-gallery-item {
        margin: 0 16px 16px 0;
        display: flex;
        flex-grow: 1;
        flex-direction: column;
        justify-content: center;
        position: relative;
        width: calc((100% - 16px * 2)/ 2);
    }

    .blocks-gallery-item figure {
        margin: 0;
    }
    
    .blocks-gallery-grid, .wp-block-gallery {
        display: flex;
        flex-wrap: wrap;
        list-style-type: none;
        padding: 0;
        margin: 0;
    }
    
    .blocks-gallery-grid.is-cropped .blocks-gallery-image a, .blocks-gallery-grid.is-cropped .blocks-gallery-image img, .blocks-gallery-grid.is-cropped .blocks-gallery-item a, .blocks-gallery-grid.is-cropped .blocks-gallery-item img, .wp-block-gallery.is-cropped .blocks-gallery-image a, .wp-block-gallery.is-cropped .blocks-gallery-image img, .wp-block-gallery.is-cropped .blocks-gallery-item a, .wp-block-gallery.is-cropped .blocks-gallery-item img {
        height: 30%;
        width: 100%;
        flex: 1;
        object-fit: cover;
    }
    """