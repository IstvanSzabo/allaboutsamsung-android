package de.maxisma.allaboutsamsung

import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import androidx.net.toUri

class LegalNoticeActivity : BaseActivity(useDefaultMenu = false) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .build()
            .launchUrl(this, BuildConfig.LEGAL_NOTICE_URL.toUri())

        finish()
    }
}