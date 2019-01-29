package de.maxisma.allaboutsamsung

import android.app.Activity
import android.content.Context
import android.support.multidex.MultiDexApplication
import android.support.v4.app.Fragment
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.JobManager
import com.google.android.gms.ads.MobileAds
import de.maxisma.allaboutsamsung.scheduling.JobCreator
import de.maxisma.allaboutsamsung.settings.migrateFromV4
import io.fabric.sdk.android.Fabric
import com.crashlytics.android.core.CrashlyticsCore



class App : MultiDexApplication() {

    val appComponent: AppComponent =
        DaggerAppComponent.builder().appModule(AppModule(this)).build()

    override fun onCreate() {
        super.onCreate()

        appComponent.preferenceHolder.migrateFromV4(this)

        Fabric.with(this, Crashlytics.Builder().core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build())

        @Suppress("SENSELESS_COMPARISON")
        if (BuildConfig.APPMOB_APP_ID != null) {
            MobileAds.initialize(this, BuildConfig.APPMOB_APP_ID)
        }

        JobManager.create(this).addJobCreator(JobCreator())
    }
}

val Context.app: App
    get() = applicationContext as App

val Activity.app: App
    get() = application as App

val Fragment.app: App
    get() = activity?.app ?: context!!.app