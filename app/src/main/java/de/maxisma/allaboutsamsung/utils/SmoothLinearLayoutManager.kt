package de.maxisma.allaboutsamsung.utils

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.DisplayMetrics

private const val DEFAULT_MS_PER_INCH = 25f

/**
 * A [LinearLayoutManager] that scrolls smoothly to items using [smoothScrollToPosition]
 */
@Suppress("unused")
class SmoothLinearLayoutManager : LinearLayoutManager {

    private val msPerInch: Float

    constructor(context: Context, msPerInch: Float = DEFAULT_MS_PER_INCH) :
            super(context) {
        this.msPerInch = msPerInch
    }

    constructor(context: Context, @RecyclerView.Orientation orientation: Int, reverseLayout: Boolean, msPerInch: Float = DEFAULT_MS_PER_INCH) :
            super(context, orientation, reverseLayout) {
        this.msPerInch = msPerInch
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int, msPerInch: Float = DEFAULT_MS_PER_INCH) :
            super(context, attrs, defStyleAttr, defStyleRes) {
        this.msPerInch = msPerInch
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun computeScrollVectorForPosition(targetPosition: Int) =
                this@SmoothLinearLayoutManager.computeScrollVectorForPosition(targetPosition)

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics) = msPerInch / displayMetrics.densityDpi
        }
        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }
}