package de.maxisma.allaboutsamsung.utils.glide

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import de.maxisma.allaboutsamsung.R

@GlideModule
class AppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        builder.setDefaultRequestOptions(
            RequestOptions()
                .error(R.drawable.ic_error_black_24dp)
        )
    }
}