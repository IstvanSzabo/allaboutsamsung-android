package de.maxisma.allaboutsamsung.posts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest
import kotlin.math.min

class DarkenTransformation : BitmapTransformation() {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.digest("darken".toByteArray())
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val new = pool.get(min(outWidth, toTransform.width), min(outHeight, toTransform.height), Bitmap.Config.RGB_565)
        val canvas = Canvas(new)
        canvas.drawBitmap(toTransform, 0f, 0f, Paint())
        canvas.drawColor(Color.argb(100, 0, 0, 0))
        return new
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }


}