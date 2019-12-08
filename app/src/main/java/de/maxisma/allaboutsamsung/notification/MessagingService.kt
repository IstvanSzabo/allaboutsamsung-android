package de.maxisma.allaboutsamsung.notification

import com.evernote.android.job.JobManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.scheduling.NOTIFICATION_JOB_TAG
import de.maxisma.allaboutsamsung.scheduling.scheduleNotificationJob
import de.maxisma.allaboutsamsung.settings.PushTopics
import de.maxisma.allaboutsamsung.utils.map
import jp.takuji31.koreference.KoreferenceModel
import jp.takuji31.koreference.stringSetPreference
import kotlinx.coroutines.runBlocking
import org.json.JSONArray

private const val prefsFile = "messaging_service"
private const val keyReceivedGuids = "received_guids"

private const val maxPendingNotifications = 15

class MessagingService : FirebaseMessagingService() {

    private val model by lazy {
        object : KoreferenceModel(this, name = prefsFile) {
            var seenGuids by stringSetPreference(default = emptySet(), key = keyReceivedGuids)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (shouldIgnoreMessages()) return

        val guid = message.data["guid"]?.toLongOrNull() ?: return

        val guidStr = guid.toString()
        if (guidStr in model.seenGuids) {
            return
        }
        model.seenGuids += guidStr

        val wildcardActive = app.appComponent.preferenceHolder.pushTopics == PushTopics.ALL
        if (wildcardActive) {
            scheduleNotificationJob(guid)
            return
        }

        val categorySlugs = JSONArray(message.data["categories"] ?: return).map { it as String }
        val tagSlugs = JSONArray(message.data["tags"] ?: return).map { it as String }
        val extraTopics = JSONArray(message.data["extraTopics"] ?: "[]").map { it as String }

        val db = app.appComponent.db

        // We're in a worker thread, so this is ok
        runBlocking {
            val subscribedCategorySlugs = db.categoryDao.subscribedCategories().map { it.slug }.toHashSet()
            val subscribedTagSlugs = db.tagDao.subscribedTags().map { it.slug }.toHashSet()

            // Let the server know about topics we are no longer interested in
            val unsubCategories = categorySlugs.filterNot { it in subscribedCategorySlugs }
            val unsubTags = tagSlugs.filterNot { it in subscribedTagSlugs }
            unsubscribe(unsubCategories, unsubTags, unsubscribeFromWildcard = !wildcardActive)

            val isDebug = DEBUG_TOPIC in extraTopics

            if (isDebug || categorySlugs.size > unsubCategories.size || tagSlugs.size > unsubTags.size) {
                scheduleNotificationJob(guid)
            }
        }
    }

    private fun shouldIgnoreMessages() =
        JobManager.instance().getAllJobRequestsForTag(NOTIFICATION_JOB_TAG).size > maxPendingNotifications

}