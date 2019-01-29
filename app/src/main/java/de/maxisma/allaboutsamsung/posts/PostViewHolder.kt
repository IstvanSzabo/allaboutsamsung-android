package de.maxisma.allaboutsamsung.posts

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import de.maxisma.allaboutsamsung.R

class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val image: ImageView = itemView.findViewById(R.id.rowPostBackground)
    val title: TextView = itemView.findViewById(R.id.rowPostTitle)
    val breakingView: TextView = itemView.findViewById(R.id.rowPostBreaking)
}