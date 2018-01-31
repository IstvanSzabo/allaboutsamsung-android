package de.maxisma.allaboutsamsung

import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import de.maxisma.allaboutsamsung.settings.PreferenceHolder
import de.maxisma.allaboutsamsung.settings.newPreferencesActivityIntent
import javax.inject.Inject

abstract class BaseActivity(private val useDefaultMenu: Boolean = true) : AppCompatActivity() {

    @Inject
    lateinit var preferenceHolder: PreferenceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app.appComponent.inject(this)

        if (preferenceHolder.useDarkTheme) {
            setTheme(R.style.AppTheme_Dark)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (useDefaultMenu) {
            menuInflater.inflate(R.menu.activity_base, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.preferences -> startActivity(newPreferencesActivityIntent(this))
            R.id.legal_notice -> CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
                .launchUrl(this, Uri.parse(BuildConfig.LEGAL_NOTICE_URL))
        }
        return super.onOptionsItemSelected(item)
    }
}