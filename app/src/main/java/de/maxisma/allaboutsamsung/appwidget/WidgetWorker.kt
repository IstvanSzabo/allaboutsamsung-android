package de.maxisma.allaboutsamsung.appwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.maxisma.allaboutsamsung.R
import java.util.concurrent.TimeUnit

const val WIDGET_JOB_TAG = "widget_job"

/**
 * Schedule a recurring job that triggers an appwidget update
 * in appropriate situations.
 */
fun Context.scheduleWidgetJob() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    val request = PeriodicWorkRequestBuilder<WidgetWorker>(1, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(this)
        .enqueueUniquePeriodicWork(WIDGET_JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, request)
}

/**
 * Cancel any recurring job that triggers appwidget updates.
 */
fun Context.unscheduleWidgetJob() {
    WorkManager.getInstance(this).cancelAllWorkByTag(WIDGET_JOB_TAG)
}

class WidgetWorker(private val context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PostsWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.postsWidgetListView)
        return Result.success()
    }
}