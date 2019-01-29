package de.maxisma.allaboutsamsung.posts

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.google.android.gms.ads.AdView
import de.maxisma.allaboutsamsung.R

class AdViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
    val adView: AdView = rootView.findViewById(R.id.rowAd)
}