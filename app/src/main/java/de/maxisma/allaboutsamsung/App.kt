package de.maxisma.allaboutsamsung

import android.app.Activity
import android.content.Context
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.multidex.MultiDexApplication
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.google.android.gms.ads.MobileAds
import de.maxisma.allaboutsamsung.settings.migrateFromV4
import io.fabric.sdk.android.Fabric


class App : MultiDexApplication() {

    val appComponent: AppComponent =
        DaggerAppComponent.builder().appModule(AppModule(this)).build()

    override fun onCreate() {
        super.onCreate()

        System.setProperty(kotlinx.coroutines.DEBUG_PROPERTY_NAME, kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON)

        appComponent.preferenceHolder.migrateFromV4(this)

        val disableCrashlytics = BuildConfig.DEBUG || appComponent.preferenceHolder.gdprMode
        Fabric.with(this, Crashlytics.Builder().core(CrashlyticsCore.Builder().disabled(disableCrashlytics).build()).build())

        @Suppress("SENSELESS_COMPARISON")
        if (BuildConfig.APPMOB_APP_ID != null) {
            MobileAds.initialize(this, BuildConfig.APPMOB_APP_ID)
        }

        if (BuildConfig.DEBUG) {
            // This is deliberately inside an if-statement as it can cause crashes
            // even when called with a false parameter
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}

val Context.app: App
    get() = applicationContext as App

val Activity.app: App
    get() = application as App

val Fragment.app: App
    get() = activity?.app ?: context!!.app