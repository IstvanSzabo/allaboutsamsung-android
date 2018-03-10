package de.maxisma.allaboutsamsung.utils.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.utils.use

/**
 * A [ViewPager] with extra features.
 *
 * @see disableTouchPaging
 */
open class ExtendedViewPager : ViewPager {

    /**
     * Whether touches should be blocked or not
     */
    var disableTouchPaging = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        disableTouchPaging = context.theme.obtainStyledAttributes(attrs, R.styleable.ExtendedViewPager, 0, 0).use {
            it.getBoolean(R.styleable.ExtendedViewPager_disableTouchPaging, disableTouchPaging)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent) = !disableTouchPaging && super.onInterceptTouchEvent(event)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent) = !disableTouchPaging && super.onTouchEvent(event)

    override fun canScrollHorizontally(direction: Int) = !disableTouchPaging && super.canScrollHorizontally(direction)

}