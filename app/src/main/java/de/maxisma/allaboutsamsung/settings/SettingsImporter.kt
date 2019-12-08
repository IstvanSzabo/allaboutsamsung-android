package de.maxisma.allaboutsamsung.settings

import android.content.Context
import androidx.preference.PreferenceManager

fun PreferenceHolder.migrateFromV4(context: Context) {
    val oldPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    if (oldPrefs.getBoolean("migrationComplete", false)) return

    val pushUpdatesFor = if ("pushUpdatesFor" in oldPrefs) {
        oldPrefs.getString("pushUpdatesFor", null)
    } else null
    val pushDeals = if ("pushDeals" in oldPrefs) {
        oldPrefs.getBoolean("pushDeals", false)
    } else null
    val disableAnalytics = if ("disableAnalytics" in oldPrefs) {
        oldPrefs.getBoolean("disableAnalytics", false)
    } else null
    val useDarkTheme = if ("useDarkTheme" in oldPrefs) {
        oldPrefs.getBoolean("useDarkTheme", false)
    } else null

    oldPrefs.edit().clear().apply()

    when (pushUpdatesFor) {
        "breaking" -> this.pushTopics = PushTopics.BREAKING
        "all" -> this.pushTopics = PushTopics.ALL
        "none" -> this.pushTopics = PushTopics.NONE
    }

    if (pushDeals != null) this.pushDeals = pushDeals
    if (disableAnalytics != null) this.allowAnalytics = !disableAnalytics
    if (useDarkTheme != null) this.useDarkThemeAlways = useDarkTheme

    oldPrefs.edit().putBoolean("migrationComplete", true).apply()
}