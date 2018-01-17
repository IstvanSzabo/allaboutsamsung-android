package de.maxisma.allaboutsamsung.utils.views

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

class DirectChildrenViewPager : ExtendedViewPager {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    private val customAdapter = object : PagerAdapter() {
        override fun instantiateItem(container: ViewGroup, position: Int) = getChildAt(position)

        override fun isViewFromObject(view: View, `object`: Any) = view == `object`

        override fun getCount() = childCount
    }

    init {
        super.setAdapter(customAdapter)
        offscreenPageLimit = childCount + 1
    }

    override fun setAdapter(adapter: PagerAdapter?): Nothing {
        throw UnsupportedOperationException("setAdapter is not supported!")
    }

    override fun addView(child: View?) {
        super.addView(child)
        customAdapter.notifyDataSetChanged()
    }

    override fun addView(child: View?, index: Int) {
        super.addView(child, index)
        customAdapter.notifyDataSetChanged()
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        super.addView(child, params)
        customAdapter.notifyDataSetChanged()
    }

    override fun addView(child: View?, width: Int, height: Int) {
        super.addView(child, width, height)
        customAdapter.notifyDataSetChanged()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        customAdapter.notifyDataSetChanged()
    }
}