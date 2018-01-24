package de.maxisma.allaboutsamsung.utils

fun <T> Collection<T>.asArrayList(): ArrayList<T> = this as? ArrayList ?: ArrayList(this)