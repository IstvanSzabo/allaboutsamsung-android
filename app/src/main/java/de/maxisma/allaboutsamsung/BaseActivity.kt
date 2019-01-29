package de.maxisma.allaboutsamsung

import android.os.Build
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import de.maxisma.allaboutsamsung.settings.PreferenceHolder
import de.maxisma.allaboutsamsung.settings.newPreferencesActivityIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * An [AppCompatActivity] that detects when the theme has changed in [onResume] and recreates
 * itself as needed.
 *
 * @param useDefaultMenu If true, automatically inflates [R.menu.activity_base] and handles clicks
 * @see [uiLaunch]
 */
abstract class BaseActivity(private val useDefaultMenu: Boolean = true) : AppCompatActivity(), CoroutineScope {

    @Inject
    lateinit var preferenceHolder: PreferenceHolder

    private var wasDarkThemeEnabled = false

    private var uiJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = uiJob + Dispatchers.Main

    override fun onStop() {
        uiJob.cancel()
        uiJob = SupervisorJob()
        super.onStop()
    }

    override fun onPause() {
        super.onPause()
        wasDarkThemeEnabled = preferenceHolder.useDarkTheme
    }

    override fun onResume() {
        super.onResume()
        if (wasDarkThemeEnabled != preferenceHolder.useDarkTheme) {
            recreate()
        }
    }

    override fun recreate() {
        // For some reason, Android calls onPause directly after onResume on older versions
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            super.recreate()
        } else {
            val intent = intent
            finish()
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        app.appComponent.inject(this)

        wasDarkThemeEnabled = preferenceHolder.useDarkTheme
        val darkThemeToUse = darkThemeToUse
        if (darkThemeToUse != null && preferenceHolder.useDarkTheme) {
            setTheme(darkThemeToUse)
        }

        super.onCreate(savedInstanceState)
    }

    @StyleRes
    protected open val darkThemeToUse: Int? = R.style.AppTheme_Dark_WithActionBar

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (useDefaultMenu) {
            menuInflater.inflate(R.menu.activity_base, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.preferences -> startActivity(newPreferencesActivityIntent(this))
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Launch [f] on the UI thread and cancel it automatically in [onStop]
     */
    protected fun uiLaunch(f: suspend CoroutineScope.() -> Unit) = launch { f() }

}