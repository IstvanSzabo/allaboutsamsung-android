package de.maxisma.allaboutsamsung.utils

import org.jsoup.parser.Parser

/**
 * Unescape HTML entities in the string
 */
fun String.htmlUnescape(): String = Parser.unescapeEntities(this, true)

/**
 * Make the string fit into [maxLength]. If it needs to be shortened, [suffix] is added while
 * still fitting into [maxLength].
 */
fun String.ellipsize(maxLength: Int, suffix: String = "…") =
    if (length <= maxLength) this else substring(0, maxLength - suffix.length) + suffix