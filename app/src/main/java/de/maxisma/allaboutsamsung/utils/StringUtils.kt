package de.maxisma.allaboutsamsung.utils

import org.jsoup.parser.Parser

fun String.htmlUnescape(): String = Parser.unescapeEntities(this, true)

fun String.ellipsize(maxLength: Int, suffix: String = "…") =
    if (length <= maxLength) this else substring(0, maxLength - suffix.length) + suffix