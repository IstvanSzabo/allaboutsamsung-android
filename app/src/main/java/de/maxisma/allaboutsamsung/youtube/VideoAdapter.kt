package de.maxisma.allaboutsamsung.youtube

import android.content.Context
import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Video
import de.maxisma.allaboutsamsung.posts.DarkenTransformation
import de.maxisma.allaboutsamsung.utils.glide.GlideApp

class VideoAdapter(var videos: List<Video> = emptyList(), private val onClick: (Video) -> Unit) : RecyclerView.Adapter<VideoViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private lateinit var transformation: MultiTransformation<Bitmap>

    private fun initTransformation(context: Context) {
        if (::transformation.isInitialized) return

        val dimensionPixelSize = context.resources.getDimensionPixelSize(R.dimen.rounded_corner_radius)
        transformation = MultiTransformation<Bitmap>(YouTubeTrimTransformation(), DarkenTransformation(), RoundedCorners(dimensionPixelSize), CenterCrop())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        initTransformation(parent.context)

        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun getItemCount() = videos.size

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.title.text = video.title
        holder.itemView.setOnClickListener { onClick(video) }

        GlideApp.with(holder.background)
            .load(video.thumbnailUrl)
            .transform(transformation)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.background)
    }

    override fun getItemId(position: Int) = videos[position].id.hashCode().toLong()

}

class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val background: ImageView = itemView.findViewById(R.id.rowVideoBackground)
    val title: TextView = itemView.findViewById(R.id.rowVideoTitle)
}