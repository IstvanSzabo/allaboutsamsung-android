package de.maxisma.allaboutsamsung.youtube

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * The height of one black bar in the YouTube thumbnail
 */
private const val TRIM_MARGIN_PERCENT = 0.125
private const val ID = "YOUTUBE_TRIM_TRANSFORMATION"

class YouTubeTrimTransformation : BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val marginPx = (TRIM_MARGIN_PERCENT * toTransform.height).roundToInt()
        return Bitmap.createBitmap(toTransform, 0, marginPx, toTransform.width, toTransform.height - 2 * marginPx)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray())
    }

}