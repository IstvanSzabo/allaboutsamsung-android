package de.maxisma.allaboutsamsung.posts

import android.content.Context
import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.ads.AdRequest
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.utils.applyStyledTitle
import de.maxisma.allaboutsamsung.utils.glide.GlideApp

private const val VIEW_TYPE_AD = 0
private const val VIEW_TYPE_POST = 1
private const val ITEM_ID_AD = -1L

class PostsAdapter(
    var posts: List<PostViewModel> = emptyList(),
    private val showAd: Boolean,
    private val onClick: (Post) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private lateinit var transformation: Transformation<Bitmap>

    private fun correctedPostPosition(position: Int) = position - (if (showAd) 1 else 0)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is PostViewHolder) return

        val postViewModel = posts[correctedPostPosition(position)]
        holder.itemView.setOnClickListener { onClick(postViewModel.post) }
        holder.title.applyStyledTitle(postViewModel.styledTitle)
        holder.breakingView.visibility = if (postViewModel.isBreaking) View.VISIBLE else View.GONE

        GlideApp.with(holder.itemView)
            .load(postViewModel.post.imageUrl)
            .transform(transformation)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.image)
    }

    private fun initTransformation(context: Context) {
        if (::transformation.isInitialized) return

        val dimensionPixelSize = context.resources.getDimensionPixelSize(R.dimen.rounded_corner_radius)
        transformation = MultiTransformation(CenterCrop(), RoundedCorners(dimensionPixelSize))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        initTransformation(parent.context)
        return when (viewType) {
            VIEW_TYPE_AD -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.row_ad, parent, false)
                AdViewHolder(view).apply {
                    adView.loadAd(AdRequest.Builder().build())
                }
            }
            VIEW_TYPE_POST -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.row_post, parent, false)
                PostViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun getItemCount() = posts.size + (if (showAd) 1 else 0)

    override fun getItemId(position: Int) = if (getItemViewType(position) == VIEW_TYPE_AD) {
        ITEM_ID_AD
    } else {
        posts[correctedPostPosition(position)].post.id
    }

    override fun getItemViewType(position: Int) = if (showAd && position == 0) VIEW_TYPE_AD else VIEW_TYPE_POST
}