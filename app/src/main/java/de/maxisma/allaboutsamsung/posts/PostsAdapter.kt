package de.maxisma.allaboutsamsung.posts

import android.content.Context
import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.utils.glide.GlideApp

class PostsAdapter(var posts: List<PostViewModel> = emptyList(), private val onClick: (Post) -> Unit) : RecyclerView.Adapter<PostViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private lateinit var transformation: MultiTransformation<Bitmap>

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val postViewModel = posts[position]
        holder.itemView.setOnClickListener { onClick(postViewModel.post) }
        holder.title.text = postViewModel.post.title
        holder.breakingView.visibility = if (postViewModel.isBreaking) View.VISIBLE else View.GONE

        GlideApp.with(holder.itemView)
            .load(postViewModel.post.imageUrl)
            .centerCrop()
            .transform(transformation)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.image)
    }

    private fun initTransformation(context: Context) {
        if (::transformation.isInitialized) return

        val dimensionPixelSize = context.resources.getDimensionPixelSize(R.dimen.rounded_corner_radius)
        transformation = MultiTransformation<Bitmap>(DarkenTransformation(), RoundedCorners(dimensionPixelSize))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        initTransformation(parent.context)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_post, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount() = posts.size

    override fun getItemId(position: Int) = posts[position].post.id
}