package de.maxisma.allaboutsamsung

import android.app.Activity
import android.content.Context
import android.support.multidex.MultiDexApplication
import android.support.v4.app.Fragment
import android.webkit.WebView
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.JobManager
import de.maxisma.allaboutsamsung.scheduling.JobCreator
import de.maxisma.allaboutsamsung.utils.IOPool
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.experimental.launch

class App : MultiDexApplication() {

    val appComponent: AppComponent =
        DaggerAppComponent.builder().appModule(AppModule(this)).build()

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        JobManager.create(this).addJobCreator(JobCreator())

        launch(IOPool) {
            appComponent.db.apply {
                postDao.deleteOld()
                videoDao.deleteExpired()
            }
        }
    }
}

val Context.app: App
    get() = applicationContext as App

val Activity.app: App
    get() = application as App

val Fragment.app: App
    get() = activity?.app ?: context!!.app