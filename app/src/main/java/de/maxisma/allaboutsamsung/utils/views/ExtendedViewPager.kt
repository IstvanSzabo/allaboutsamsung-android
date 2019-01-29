package de.maxisma.allaboutsamsung.utils.views

import android.annotation.SuppressLint
import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.res.use
import de.maxisma.allaboutsamsung.R

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