package de.maxisma.allaboutsamsung.posts

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import de.maxisma.allaboutsamsung.R

class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val image = itemView.findViewById<ImageView>(R.id.rowPostBackground)
    val title = itemView.findViewById<TextView>(R.id.rowPostTitle)
}