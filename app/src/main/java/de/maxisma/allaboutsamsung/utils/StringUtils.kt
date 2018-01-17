package de.maxisma.allaboutsamsung.utils

import org.jsoup.parser.Parser

fun String.htmlUnescape(): String = Parser.unescapeEntities(this, true)