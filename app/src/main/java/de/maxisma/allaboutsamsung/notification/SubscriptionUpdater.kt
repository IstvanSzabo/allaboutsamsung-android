package de.maxisma.allaboutsamsung.notification

import android.arch.lifecycle.Transformations.map
import com.google.firebase.messaging.FirebaseMessaging
import de.maxisma.allaboutsamsung.db.Category
import de.maxisma.allaboutsamsung.db.Tag
import de.maxisma.allaboutsamsung.utils.IOPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

fun updatePushSubscription() {
    // TODO Get subs from db, call subscribe
}

private fun categorySlugToTopic(slug: String) = "category~%$slug"
private fun tagSlugToTopic(slug: String) = "tag~%$slug"

fun subscribe(categories: List<Category>, tags: List<Tag>) {
    launch(IOPool) {
        val fb = FirebaseMessaging.getInstance()
        categories.asSequence().map { categorySlugToTopic(it.slug) }.forEach { fb.subscribeToTopic(it) }
        tags.asSequence().map { tagSlugToTopic(it.slug) }.forEach { fb.subscribeToTopic(it) }
    }
}

fun unsubscribe(categorySlugs: List<String>, tagSlugs: List<String>) {
    launch(IOPool) {
        val fb = FirebaseMessaging.getInstance()
        categorySlugs.asSequence().map { categorySlugToTopic(it) }.forEach { fb.unsubscribeFromTopic(it) }
        tagSlugs.asSequence().map { tagSlugToTopic(it) }.forEach { fb.unsubscribeFromTopic(it) }
    }
}