package de.maxisma.allaboutsamsung.utils

import android.content.res.Resources
import android.view.View

fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun View.toggleVisibility(disabledState: Int = View.GONE) {
    require(disabledState == View.GONE || disabledState == View.INVISIBLE) { "disabledState must be either GONE or INVISIBLE!" }
    visibility = if (visibility == View.VISIBLE) disabledState else View.VISIBLE
}