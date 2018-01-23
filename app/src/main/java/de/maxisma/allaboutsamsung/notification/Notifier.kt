package de.maxisma.allaboutsamsung.notification

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

fun notifyAboutPost(guid: PostId, db: Db, api: WordpressApi, context: Context) {
    val query = Query.Filter(string = null, onlyCategories = null, onlyTags = null, onlyIds = listOf(guid))
    val executor = query.newExecutor(api, db, onError = { TODO("Handle") })
    launch(UI) {
        executor.data.observeForever(notifier(executor.data, context))
        executor.requestNewerPosts()
    }
}

private fun notifier(data: LiveData<List<Post>>, context: Context) = object : Observer<List<Post>> {
    override fun onChanged(value: List<Post>?) {
        value ?: return
        require(value.size == 1) { "Expected only one post to notify about!" }
        data.removeObserver(this)
        value.first().notifyAboutPost(context)
    }
}

private fun Post.notifyAboutPost(context: Context) {
    TODO()
}