package de.maxisma.allaboutsamsung.gallery

import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import de.maxisma.allaboutsamsung.BaseActivity
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.utils.IOPool
import de.maxisma.allaboutsamsung.utils.asArrayList
import de.maxisma.allaboutsamsung.utils.glide.GlideApp
import de.maxisma.allaboutsamsung.utils.toggleVisibility
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import kotlin.math.max

private const val EXTRA_PHOTOS = "photos"
private const val EXTRA_SELECTED_PHOTO = "selected_photo"

fun newGalleryActivityIntent(context: Context, photos: List<Photo>, selectedPhoto: Photo? = null): Intent =
    Intent(context, GalleryActivity::class.java).apply {
        putParcelableArrayListExtra(EXTRA_PHOTOS, photos.asArrayList())
        putExtra(EXTRA_SELECTED_PHOTO, selectedPhoto)
    }

class GalleryActivity : BaseActivity(useDefaultMenu = false) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val photos = intent.getParcelableArrayListExtra<Photo>(EXTRA_PHOTOS)
        val selectedPhoto = intent.getParcelableExtra<Photo?>(EXTRA_SELECTED_PHOTO)
        val selectedIndex = max(0, photos.indexOf(selectedPhoto))

        launch(UI) {
            val photoBar = galleryPhotoBar.configurePhotoBar(photos, onPhotoClick = { photo, photoBar ->
                galleryViewPager.currentItem = photos.indexOf(photo)
                photoBar.highlightPhoto(photo)
            }).receive()

            galleryViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    photoBar.highlightPhoto(photos[position])
                }
            })

            galleryViewPager.adapter = PhotoAdapter(
                photos,
                onClick = { galleryPhotoBar.toggleVisibility(disabledState = View.GONE) },
                onZoomedInStateChanged = { zoomedIn ->
                    galleryPhotoBar.visibility = if (!zoomedIn) View.VISIBLE else View.GONE
                    galleryViewPager.disableTouchPaging = zoomedIn
                }
            )
            galleryViewPager.currentItem = selectedIndex
        }
    }
}

@PaperParcel
data class Photo(val smallImageUrl: String, val fullImageUrl: String) : PaperParcelable {
    companion object {
        @JvmField
        val CREATOR = PaperParcelPhoto.CREATOR
    }
}

private class PhotoAdapter(private val photos: List<Photo>, private val onClick: () -> Unit, private val onZoomedInStateChanged: (Boolean) -> Unit) : PagerAdapter() {

    private fun photoView(context: Context, photo: Photo): View = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setOnClickListener { onClick() }

        val progressBar = ProgressBar(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            isIndeterminate = true
            visibility = View.GONE
        }

        val imageView = SubsamplingScaleImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setOnStateChangedListener(object : SubsamplingScaleImageView.OnStateChangedListener {
                override fun onCenterChanged(p0: PointF?, p1: Int) {}

                override fun onScaleChanged(scale: Float, p1: Int) {
                    onZoomedInStateChanged(scale > minScale)
                }
            })

            progressBar.visibility = View.VISIBLE
            launch(IOPool) {
                val resource = GlideApp.with(context)
                    .asFile()
                    .load(photo.fullImageUrl)
                    .submit()
                    .get()
                launch(UI) {
                    if (isAttachedToWindow) {
                        progressBar.visibility = View.GONE
                        setImage(ImageSource.uri("file://$resource"))
                    }
                }
            }
        }

        addView(imageView)
        addView(progressBar)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = photoView(container.context, photos[position])
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any) = view === `object`

    override fun getCount() = photos.size

}