package de.maxisma.allaboutsamsung.utils

import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.widget.TextView
import de.maxisma.allaboutsamsung.R

typealias StyledTitle = CharSequence

fun String.toStyledTitle(context: Context): StyledTitle {
    val color = ContextCompat.getColor(context, R.color.titleBackgroundColor)
    val padding = context.resources.getDimensionPixelSize(R.dimen.title_background_padding)
    return SpannableString(this).apply {
        setSpan(PaddingBackgroundColorSpan(color, padding), 0, length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
    }
}

fun TextView.applyStyledTitle(styledTitle: StyledTitle) {
    val titleBackgroundPadding = context.resources.getDimensionPixelSize(R.dimen.title_background_padding)

    text = styledTitle
    setShadowLayer(titleBackgroundPadding.toFloat(), 0f, 0f, 0)
    setPadding(titleBackgroundPadding, titleBackgroundPadding, titleBackgroundPadding, titleBackgroundPadding)
}