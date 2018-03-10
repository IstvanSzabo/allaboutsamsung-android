package de.maxisma.allaboutsamsung

import android.os.Build
import android.os.Bundle
import android.support.annotation.StyleRes
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import de.maxisma.allaboutsamsung.settings.PreferenceHolder
import de.maxisma.allaboutsamsung.settings.newPreferencesActivityIntent
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

/**
 * An [AppCompatActivity] that detects when the theme has changed in [onResume] and recreates
 * itself as needed.
 *
 * @param useDefaultMenu If true, automatically inflates [R.menu.activity_base] and handles clicks
 * @see [uiLaunch]
 */
abstract class BaseActivity(private val useDefaultMenu: Boolean = true) : AppCompatActivity() {

    @Inject
    lateinit var preferenceHolder: PreferenceHolder

    private var wasDarkThemeEnabled = false

    override fun onPause() {
        val cause = CancellationException("onPause")
        synchronized(uiJobs) {
            uiJobs.forEach { it.cancel(cause) }
            uiJobs.clear()
        }

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
            val app = applicationContext
            val intent = intent
            finish()
            app.startActivity(intent)
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
    protected open val darkThemeToUse: Int? = R.style.AppTheme_Dark

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

    private val uiJobs = mutableListOf<Job>()

    /**
     * Launch [f] on the UI thread and cancel it automatically in [onPause]
     */
    fun uiLaunch(f: suspend CoroutineScope.() -> Unit): Job {
        val job = launch(UI) { f() }

        synchronized(uiJobs) {
            uiJobs += job
        }
        job.invokeOnCompletion {
            synchronized(uiJobs) {
                uiJobs -= job
            }
        }

        return launch { job.join() }
    }

}