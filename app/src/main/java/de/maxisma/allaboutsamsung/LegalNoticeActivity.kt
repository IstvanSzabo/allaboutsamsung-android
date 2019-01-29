package de.maxisma.allaboutsamsung

import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

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