package de.maxisma.allaboutsamsung.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.utils.map
import org.json.JSONArray

class MessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val guid = message.data["guid"]?.toLongOrNull() ?: return
        val categorySlugs = JSONArray(message.data["categories"] ?: return).map { it as String }
        val tagSlugs = JSONArray(message.data["tags"] ?: return).map { it as String }

        val db = app.appComponent.db
        val api = app.appComponent.wordpressApi

        val subscribedCategorySlugs = db.categoryDao.subscribedCategories().map { it.slug }.toHashSet()
        val subscribedTagSlugs = db.tagDao.subscribedTags().map { it.slug }.toHashSet()
        val unsubCategories = categorySlugs.filterNot { it in subscribedCategorySlugs }
        val unsubTags = tagSlugs.filterNot { it in subscribedTagSlugs }

        unsubscribe(unsubCategories, unsubTags)

        if (subscribedCategorySlugs.size > unsubCategories.size || subscribedTagSlugs.size > unsubTags.size) {
            notifyAboutPost(guid, db, api, applicationContext)
        }
    }


}