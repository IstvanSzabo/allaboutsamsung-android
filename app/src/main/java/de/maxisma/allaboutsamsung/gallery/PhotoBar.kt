package de.maxisma.allaboutsamsung.gallery

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.ImageView
import de.maxisma.allaboutsamsung.utils.SmoothLinearLayoutManager
import de.maxisma.allaboutsamsung.utils.glide.GlideApp
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch

private const val PHOTOS_ON_SCREEN = 3

class PhotoBar(private val recyclerView: RecyclerView, private val photos: List<Photo>) {
    fun highlightPhoto(photo: Photo) {
        recyclerView.smoothScrollToPosition(photos.indexOf(photo))
        // TODO Highlight
    }
}

/**
 * Sends exactly one [PhotoBar]
 */
fun RecyclerView.configurePhotoBar(photos: List<Photo>, onPhotoClick: (Photo, PhotoBar) -> Unit): ReceiveChannel<PhotoBar> = produce(UI) {
    if (width == 0) {
        launch(UI) {
            send(configurePhotoBar(photos, onPhotoClick).receive())
        }
        return@produce
    }

    val photoBar = PhotoBar(this@configurePhotoBar, photos)
    layoutManager = SmoothLinearLayoutManager(this@configurePhotoBar.context, LinearLayoutManager.HORIZONTAL, false, 100f)
    adapter = PhotoBarAdapter(photos, onPhotoClick = { onPhotoClick(it, photoBar) })
    send(photoBar)
}

private class PhotoBarViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

private class PhotoBarAdapter(private val photos: List<Photo>, private val onPhotoClick: (Photo) -> Unit) : RecyclerView.Adapter<PhotoBarViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PhotoBarViewHolder(ImageView(parent.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            parent.width / PHOTOS_ON_SCREEN,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    })

    override fun getItemCount() = photos.size

    override fun onBindViewHolder(holder: PhotoBarViewHolder, position: Int) {
        GlideApp.with(holder.imageView)
            .load(photos[position].smallImageUrl)
            .centerCrop()
            .into(holder.imageView)
        holder.imageView.setOnClickListener { onPhotoClick(photos[position]) }
    }
}