package de.maxisma.allaboutsamsung.utils

import android.app.Activity
import android.content.res.Configuration
import androidx.fragment.app.Fragment

val Activity.isSystemDarkModeActive get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

val Fragment.isSystemDarkModeActive get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES