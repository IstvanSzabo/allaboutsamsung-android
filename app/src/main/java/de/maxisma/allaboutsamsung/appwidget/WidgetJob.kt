package de.maxisma.allaboutsamsung.appwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import de.maxisma.allaboutsamsung.R
import java.util.concurrent.TimeUnit

const val WIDGET_JOB_TAG = "widget_job"

/**
 * Schedule a recurring job that triggers an appwidget update
 * in appropriate situations.
 */
fun scheduleWidgetJob() {
    JobRequest.Builder(WIDGET_JOB_TAG)
        .setPeriodic(TimeUnit.HOURS.toMillis(1))
        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .setRequirementsEnforced(true)
        .setUpdateCurrent(true)
        .build()
        .schedule()
}

/**
 * Cancel any recurring job that triggers appwidget updates.
 */
fun unscheduleWidgetJob() {
    JobManager.instance().cancelAllForTag(WIDGET_JOB_TAG)
}

class WidgetJob : Job() {
    override fun onRunJob(params: Params): Result {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PostsWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.postsWidgetListView)
        return Result.SUCCESS
    }
}