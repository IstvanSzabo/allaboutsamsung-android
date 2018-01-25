package de.maxisma.allaboutsamsung.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import java.util.WeakHashMap
import javax.inject.Inject

class PreferenceHolder @Inject constructor(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // KEEP DEFAULT VALUES IN SYNC WIT preferences.xml

    val useDarkTheme: Boolean get() = prefs.getBoolean("useDarkTheme", false)
    val allowAnalytics: Boolean get() = prefs.getBoolean("allowAnalytics", true)
    val pushTopics: PushTopics
        get() = when (prefs.getString("pushTopics", "breaking")) {
            "none" -> PushTopics.NONE
            "breaking" -> PushTopics.BREAKING
            "all" -> PushTopics.ALL
            else -> throw IllegalArgumentException("Unknown push topic!")
        }
    val pushDeals: Boolean get() = prefs.getBoolean("pushDeals", false)

    private val listeners: MutableMap<() -> Unit, SharedPreferences.OnSharedPreferenceChangeListener> = WeakHashMap()

    fun registerListener(f: () -> Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> f() }
        listeners[f] = listener
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(f: () -> Unit) {
        listeners[f]?.let { prefs.unregisterOnSharedPreferenceChangeListener(it) }
        listeners -= f
    }
}

enum class PushTopics {
    NONE, BREAKING, ALL
}