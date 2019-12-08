package de.maxisma.allaboutsamsung.scheduling

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.notification.notifyAboutPost
import de.maxisma.allaboutsamsung.rest.WordpressApi
import javax.inject.Inject

const val NOTIFICATION_JOB_TAG = "notification_job"
private const val EXTRA_POST_ID = "post_id"

/**
 * Show a notification for the post with a sensible amount of waiting time.
 * It's not done immediately to save battery, as the post needs to be downloaded
 * including an image.
 */
fun Context.scheduleNotificationJob(postId: PostId) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = OneTimeWorkRequestBuilder<NotificationWorker>()
        .setInputData(workDataOf(EXTRA_POST_ID to postId))
        .setConstraints(constraints)
        .addTag(NOTIFICATION_JOB_TAG)
        .build()

    WorkManager.getInstance(this).enqueue(request)
}

/**
 * Calls [notifyAboutPost]
 */
class NotificationWorker(private val context: Context, private val params: WorkerParameters) : CoroutineWorker(context, params) {

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var api: WordpressApi

    @Inject
    lateinit var keyValueStore: KeyValueStore

    override suspend fun doWork(): Result {
        val postId: PostId = params.inputData.getLong(EXTRA_POST_ID, -1)
        require(postId != -1L) { "Invalid postId!" }

        context.app.appComponent.inject(this)

        return notifyAboutPost(postId, db, api, context, keyValueStore)
    }

}