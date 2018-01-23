package de.maxisma.allaboutsamsung.utils

import org.json.JSONArray

inline fun <T> JSONArray.map(f: (Any) -> T): List<T> = MutableList(length()) { f(this[it]) }