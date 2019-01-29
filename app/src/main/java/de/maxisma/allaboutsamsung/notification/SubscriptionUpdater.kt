package de.maxisma.allaboutsamsung.notification

import com.google.firebase.messaging.FirebaseMessaging
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.db.Category
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Tag
import de.maxisma.allaboutsamsung.utils.IOPool
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun updatePushSubscription(db: Db, wildcard: Boolean) {
    withContext(IOPool) {
        val subscribedCategories = db.categoryDao.subscribedCategories()
        val subscribedTags = db.tagDao.subscribedTags()
        subscribe(subscribedCategories, subscribedTags, wildcard)
    }
}

const val WILDCARD_TOPIC = "_"
const val DEBUG_TOPIC = "___DEBUG___"
private fun categorySlugToTopic(slug: String) = "category~%$slug"
private fun tagSlugToTopic(slug: String) = "tag~%$slug"

/**
 * Subscribe to the given [categories] and [tags] using Firebase topics.
 *
 * @param wildcard If true, subscribe to the wildcard topic that receives every post
 */
fun subscribe(categories: List<Category>, tags: List<Tag>, wildcard: Boolean) {
    GlobalScope.launch(IOPool) {
        val fb = FirebaseMessaging.getInstance()
        categories.asSequence().map { categorySlugToTopic(it.slug) }.forEach { fb.subscribeToTopic(it) }
        tags.asSequence().map { tagSlugToTopic(it.slug) }.forEach { fb.subscribeToTopic(it) }

        if (wildcard) {
            fb.subscribeToTopic(WILDCARD_TOPIC)
        } else {
            fb.unsubscribeFromTopic(WILDCARD_TOPIC)
        }

        if (BuildConfig.DEBUG) {
            fb.subscribeToTopic(DEBUG_TOPIC)
        } else {
            fb.unsubscribeFromTopic(DEBUG_TOPIC)
        }
    }
}

/**
 * See [subscribe]
 */
fun unsubscribe(categorySlugs: List<String> = emptyList(), tagSlugs: List<String> = emptyList(), unsubscribeFromWildcard: Boolean) {
    GlobalScope.launch(IOPool) {
        val fb = FirebaseMessaging.getInstance()
        categorySlugs.asSequence().map { categorySlugToTopic(it) }.forEach { fb.unsubscribeFromTopic(it) }
        tagSlugs.asSequence().map { tagSlugToTopic(it) }.forEach { fb.unsubscribeFromTopic(it) }
        if (unsubscribeFromWildcard) fb.unsubscribeFromTopic(WILDCARD_TOPIC)
    }
}