package de.maxisma.allaboutsamsung.gallery

import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import de.maxisma.allaboutsamsung.BaseActivity
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.utils.IOPool
import de.maxisma.allaboutsamsung.utils.asArrayList
import de.maxisma.allaboutsamsung.utils.glide.GlideApp
import de.maxisma.allaboutsamsung.utils.toggleVisibility
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import kotlin.math.max

private const val EXTRA_PHOTOS = "photos"
private const val EXTRA_SELECTED_PHOTO = "selected_photo"

/**
 * Creates an [Intent] that starts the gallery with the given list of [photos].
 *
 * @param selectedPhoto If non-null, the gallery focuses on this photo after launch
 */
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

        uiLaunch {
            val photoBar = galleryPhotoBar.configurePhotoBar(photos, onPhotoClick = { photo, photoBar ->
                galleryViewPager.currentItem = photos.indexOf(photo)
                photoBar.highlightPhoto(photo)
            })

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
                },
                coroutineScope = this@GalleryActivity
            )
            galleryViewPager.currentItem = selectedIndex
        }
    }
}

@Parcelize
data class Photo(val smallImageUrl: String, val fullImageUrl: String, val others: List<String> = emptyList()) : Parcelable {
    fun isContainedByUrl(url: String) = url == smallImageUrl || url == fullImageUrl || url in others
}

/**
 * Each page is a photo that can be zoomed and clicked.
 */
private class PhotoAdapter(
    private val photos: List<Photo>,
    private val onClick: () -> Unit,
    private val onZoomedInStateChanged: (Boolean) -> Unit,
    coroutineScope: CoroutineScope
) : PagerAdapter(), CoroutineScope by coroutineScope {

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
            launch {
                val resource = withContext(IOPool) {
                    try {
                        GlideApp.with(context)
                            .asFile()
                            .load(photo.fullImageUrl)
                            .submit()
                            .get()
                    } catch (e: CancellationException) {
                        e.printStackTrace()
                        null
                    } catch (e: ExecutionException) {
                        e.printStackTrace()
                        null
                    }
                }

                progressBar.visibility = View.GONE
                if (resource != null) {
                    setImage(ImageSource.uri("file://$resource"))
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