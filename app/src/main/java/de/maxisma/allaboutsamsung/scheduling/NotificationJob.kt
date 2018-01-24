package de.maxisma.allaboutsamsung.scheduling

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.notification.notifyAboutPost

const val NOTIFICATION_JOB_TAG = "notification_job"
private const val EXTRA_POST_ID = "post_id"
private const val MAX_NOTIFICATION_WAIT_TIME_MS = 15 * 60 * 1000L

fun scheduleNotificationJob(postId: PostId) {
    JobRequest.Builder(NOTIFICATION_JOB_TAG)
        .setExtras(PersistableBundleCompat().apply {
            putLong(EXTRA_POST_ID, postId)
        })
        .setExecutionWindow(1, MAX_NOTIFICATION_WAIT_TIME_MS)
        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
        .build()
        .schedule()
}

class NotificationJob : Job() {
    override fun onRunJob(params: Params): Result {
        val postId: PostId = params.extras.getLong(EXTRA_POST_ID, -1)
        require(postId != -1L) { "Invalid postId!" }

        val db = context.app.appComponent.db
        val api = context.app.appComponent.wordpressApi
        return notifyAboutPost(postId, db, api, context)
    }
}