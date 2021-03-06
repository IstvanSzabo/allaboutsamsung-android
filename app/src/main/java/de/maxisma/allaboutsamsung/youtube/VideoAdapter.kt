package de.maxisma.allaboutsamsung.youtube

import android.content.Context
import android.graphics.Bitmap
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Video
import de.maxisma.allaboutsamsung.utils.applyStyledTitle
import de.maxisma.allaboutsamsung.utils.glide.GlideApp

class VideoAdapter(var videos: List<VideoViewModel> = emptyList(), private val onClick: (Video) -> Unit) : RecyclerView.Adapter<VideoViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private lateinit var transformation: Transformation<Bitmap>

    private fun initTransformation(context: Context) {
        if (::transformation.isInitialized) return

        val dimensionPixelSize = context.resources.getDimensionPixelSize(R.dimen.rounded_corner_radius)
        transformation = MultiTransformation<Bitmap>(YouTubeTrimTransformation(), CenterCrop(), RoundedCorners(dimensionPixelSize))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        initTransformation(parent.context)

        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun getItemCount() = videos.size

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoViewModel = videos[position]
        holder.title.applyStyledTitle(videoViewModel.styledTitle)
        holder.itemView.setOnClickListener { onClick(videoViewModel.video) }

        GlideApp.with(holder.background)
            .load(videoViewModel.video.thumbnailUrl)
            .transform(transformation)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.background)
    }

    override fun getItemId(position: Int) = videos[position].video.id.hashCode().toLong()

}

class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val background: ImageView = itemView.findViewById(R.id.rowVideoBackground)
    val title: TextView = itemView.findViewById(R.id.rowVideoTitle)
}