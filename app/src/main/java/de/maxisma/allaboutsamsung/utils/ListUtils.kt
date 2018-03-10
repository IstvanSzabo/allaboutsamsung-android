package de.maxisma.allaboutsamsung.utils

/**
 * If this is already an [ArrayList], return it, otherwise create a copy
 */
fun <T> Collection<T>.asArrayList(): ArrayList<T> = this as? ArrayList ?: ArrayList(this)