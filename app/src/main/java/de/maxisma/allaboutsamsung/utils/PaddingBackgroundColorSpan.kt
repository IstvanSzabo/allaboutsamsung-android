package de.maxisma.allaboutsamsung.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.annotation.Px
import android.text.style.LineBackgroundSpan
import kotlin.math.roundToInt

/**
 * A [LineBackgroundSpan] with padding that improves the design
 */
class PaddingBackgroundColorSpan(@ColorInt private val backgroundColor: Int, @Px private val padding: Int) : LineBackgroundSpan {
    private val mBgRect = Rect()

    override fun drawBackground(c: Canvas, p: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence, start: Int, end: Int, lnum: Int) {
        val textWidth = p.measureText(text, start, end).roundToInt()
        val paintColor = p.color
        mBgRect.set(
            left - padding,
            top - if (lnum == 0) padding / 2 else -(padding / 2),
            left + textWidth + padding,
            bottom + padding / 2
        )
        p.color = backgroundColor
        c.drawRect(mBgRect, p)
        p.color = paintColor
    }
}