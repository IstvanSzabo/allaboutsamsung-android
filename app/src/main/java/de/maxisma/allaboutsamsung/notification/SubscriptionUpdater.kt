package de.maxisma.allaboutsamsung.notification

import com.google.firebase.messaging.FirebaseMessaging
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.db.Category
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Tag
import de.maxisma.allaboutsamsung.utils.IOPool
import kotlinx.coroutines.experimental.launch

fun updatePushSubscription(db: Db) {
    launch(IOPool) {
        val subscribedCategories = db.categoryDao.subscribedCategories()
        val subscribedTags = db.tagDao.subscribedTags()
        subscribe(subscribedCategories, subscribedTags)
    }
}

const val DEBUG_TOPIC = "___DEBUG___"
private fun categorySlugToTopic(slug: String) = "category~%$slug"
private fun tagSlugToTopic(slug: String) = "tag~%$slug"

fun subscribe(categories: List<Category>, tags: List<Tag>) {
    launch(IOPool) {
        val fb = FirebaseMessaging.getInstance()
        categories.asSequence().map { categorySlugToTopic(it.slug) }.forEach { fb.subscribeToTopic(it) }
        tags.asSequence().map { tagSlugToTopic(it.slug) }.forEach { fb.subscribeToTopic(it) }

        if (BuildConfig.DEBUG) {
            fb.subscribeToTopic(DEBUG_TOPIC)
        } else {
            fb.unsubscribeFromTopic(DEBUG_TOPIC)
        }
    }
}

fun unsubscribe(categorySlugs: List<String>, tagSlugs: List<String>) {
    launch(IOPool) {
        val fb = FirebaseMessaging.getInstance()
        categorySlugs.asSequence().map { categorySlugToTopic(it) }.forEach { fb.unsubscribeFromTopic(it) }
        tagSlugs.asSequence().map { tagSlugToTopic(it) }.forEach { fb.unsubscribeFromTopic(it) }
    }
}