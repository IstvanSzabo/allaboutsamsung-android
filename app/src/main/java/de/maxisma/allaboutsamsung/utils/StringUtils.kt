package de.maxisma.allaboutsamsung.utils

import java.net.URLDecoder

fun String.urlDecode(): String = URLDecoder.decode(this, Charsets.UTF_8.name())