package de.maxisma.allaboutsamsung.gallery

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import de.maxisma.allaboutsamsung.utils.SmoothLinearLayoutManager
import de.maxisma.allaboutsamsung.utils.glide.GlideApp
import kotlinx.coroutines.android.awaitFrame

private const val PHOTOS_ON_SCREEN = 3
private val OVERLAY_COLOR = Color.argb(75, 0, 0, 0)

class PhotoBar(private val recyclerView: RecyclerView, private val photoViewModels: List<PhotoViewModel>) {

    /**
     * Fade out all but this photo.
     */
    fun highlightPhoto(photo: Photo) {
        val vmIndex = photoViewModels.indexOfFirst { it.photo == photo }
        recyclerView.smoothScrollToPosition(vmIndex)
        for (i in photoViewModels.indices) {
            photoViewModels[i].isHighlighted.value = i == vmIndex
        }
    }
}

class PhotoViewModel(val photo: Photo, val isHighlighted: MutableLiveData<Boolean>) {
    var currentObserver: Observer<Boolean>? = null
}

/**
 * Returns a [PhotoBar]. It is created after the UI has been laid out, which is done
 * because in [PhotoBarAdapter.onCreateViewHolder] the width of the bar needs to be known, as
 * we want a fixed number of photos per screen.
 */
suspend fun RecyclerView.configurePhotoBar(photos: List<Photo>, onPhotoClick: (Photo, PhotoBar) -> Unit): PhotoBar {
    while (width == 0) {
        awaitFrame()
    }

    val viewModels = photos.mapIndexed { index, photo -> PhotoViewModel(photo, MutableLiveData<Boolean>().apply { value = index == 0 }) }

    val photoBar = PhotoBar(this@configurePhotoBar, viewModels)
    layoutManager = SmoothLinearLayoutManager(this@configurePhotoBar.context, LinearLayoutManager.HORIZONTAL, false, 100f)
    adapter = PhotoBarAdapter(viewModels, onPhotoClick = { onPhotoClick(it, photoBar) })
    return photoBar
}

private class PhotoBarViewHolder(val imageView: ImageView, val overlayView: View, itemView: View) : RecyclerView.ViewHolder(itemView)

private class PhotoBarAdapter(
    private val photoViewModels: List<PhotoViewModel>,
    private val onPhotoClick: (Photo) -> Unit
) : RecyclerView.Adapter<PhotoBarViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoBarViewHolder {
        val container = FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                parent.width / PHOTOS_ON_SCREEN,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val overlayView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setImageDrawable(ColorDrawable(OVERLAY_COLOR))
        }

        container.addView(imageView)
        container.addView(overlayView)

        return PhotoBarViewHolder(imageView, overlayView, container)
    }

    override fun getItemCount() = photoViewModels.size

    override fun onBindViewHolder(holder: PhotoBarViewHolder, position: Int) {
        val viewModel = photoViewModels[position]
        GlideApp.with(holder.imageView)
            .load(viewModel.photo.smallImageUrl)
            .centerCrop()
            .into(holder.imageView)
        holder.imageView.setOnClickListener { onPhotoClick(viewModel.photo) }

        val observer = Observer<Boolean> { isHighlighted ->
            holder.overlayView.visibility = if (isHighlighted == true) View.GONE else View.VISIBLE
        }
        viewModel.currentObserver?.let { viewModel.isHighlighted.removeObserver(it) }
        viewModel.currentObserver = observer
        viewModel.isHighlighted.observeForever(observer)
    }
}