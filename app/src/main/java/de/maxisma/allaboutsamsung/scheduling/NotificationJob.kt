package de.maxisma.allaboutsamsung.scheduling

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.notification.notifyAboutPost
import de.maxisma.allaboutsamsung.rest.WordpressApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

const val NOTIFICATION_JOB_TAG = "notification_job"
private const val EXTRA_POST_ID = "post_id"
private const val MAX_NOTIFICATION_WAIT_TIME_MS = 15 * 60 * 1000L

/**
 * Show a notification for the post with a sensible amount of waiting time.
 * It's not done immediately to save battery, as the post needs to be downloaded
 * including an image.
 */
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

/**
 * Calls [notifyAboutPost]
 */
class NotificationJob : Job(), CoroutineScope {

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var api: WordpressApi

    @Inject
    lateinit var keyValueStore: KeyValueStore

    private var job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onRunJob(params: Params): Result {
        val postId: PostId = params.extras.getLong(EXTRA_POST_ID, -1)
        require(postId != -1L) { "Invalid postId!" }

        context.app.appComponent.inject(this)

        return notifyAboutPost(postId, db, api, context, keyValueStore)
    }

    override fun onCancel() {
        job.cancel()
        job = SupervisorJob()
        super.onCancel()
    }

}