package de.maxisma.allaboutsamsung.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import javax.inject.Inject

// TODO Migrate to use KoreferenceModel like KeyValueStore

/**
 * Holds the values stored by the [PreferenceFragment]
 */
class PreferenceHolder @Inject constructor(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // KEEP DEFAULT VALUES IN SYNC WITH preferences.xml

    var useDarkThemeAlways: Boolean
        get() = prefs.getBoolean("useDarkTheme", false)
        set(value) = prefs.edit().putBoolean("useDarkTheme", value).apply()

    var allowAnalytics: Boolean
        get() = prefs.getBoolean("allowAnalytics", true)
        set(value) = prefs.edit().putBoolean("allowAnalytics", value).apply()

    var pushTopics: PushTopics
        get() = when (prefs.getString("pushTopics", "breaking")) {
            "none" -> PushTopics.NONE
            "breaking" -> PushTopics.BREAKING
            "all" -> PushTopics.ALL
            else -> throw IllegalArgumentException("Unknown push topic!")
        }
        set(value) = prefs.edit().putString(
            "pushTopics", when (value) {
                PushTopics.NONE -> "none"
                PushTopics.BREAKING -> "breaking"
                PushTopics.ALL -> "all"
            }
        ).apply()

    var pushDeals: Boolean
        get() = prefs.getBoolean("pushDeals", false)
        set(value) = prefs.edit().putBoolean("pushDeals", value).apply()

    var gdprMode: Boolean
        get() = prefs.getBoolean("gdprMode", false)
        set(value) = prefs.edit().putBoolean("gdprMode", value).apply()

    private val listeners: MutableMap<() -> Unit, SharedPreferences.OnSharedPreferenceChangeListener> = mutableMapOf()

    /**
     * [f] is notified about any changes made to the preferences contained by this [PreferenceHolder]
     */
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