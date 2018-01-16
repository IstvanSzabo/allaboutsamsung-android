package de.maxisma.allaboutsamsung.posts

import android.graphics.Rect
import android.support.annotation.Px
import android.support.v7.widget.RecyclerView
import android.view.View

class SpacingItemDecoration(@Px val horizontalSpacing: Int, @Px val verticalSpacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = horizontalSpacing
        outRect.right = horizontalSpacing
        outRect.bottom = verticalSpacing

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = verticalSpacing
        }
    }
}