package de.maxisma.allaboutsamsung.appwidget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.post.newPostActivityFillInIntent
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.utils.glide.GlideApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min
import kotlin.random.Random

/**
 * Maximum number of posts to show in the appwidget.
 */
private const val MAX_ITEMS = 10
private const val IMAGE_DOWNLOAD_TIMEOUT_MS = 5000L

class PostsWidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    @Inject
    lateinit var wordpressApi: WordpressApi

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var keyValueStore: KeyValueStore

    @Volatile
    private var posts = emptyList<Post>()

    override fun onCreate() {
        context.app.appComponent.inject(this)
    }

    override fun getLoadingView() = null

    override fun getItemId(position: Int): PostId {
        val posts = posts
        return if (position !in posts.indices) {
            // Sometimes the collection size known to the homescreen becomes
            // out of sync with the actual size after a service restart / refresh
            Random.nextLong(Long.MIN_VALUE, 0)
        } else {
            posts[position].id
        }
    }

    override fun onDataSetChanged() {
        try {
            runBlocking(Dispatchers.Main) {
                val query = Query.Empty
                val executor = query.newExecutor(wordpressApi, db, keyValueStore, coroutineScope = this, onError = { coroutineContext.cancel() })

                try {
                    executor.requestNewerPosts().join()
                    posts = executor.dataImmediate()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Don't do anything, this is periodically run anyway
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Don't do anything, this is periodically run anyway
        }
    }

    override fun hasStableIds() = true

    override fun getViewAt(position: Int): RemoteViews {
        val posts = posts
        if (position !in posts.indices) {
            // Sometimes the collection size known to the homescreen becomes
            // out of sync with the actual size after a service restart / refresh
            return RemoteViews(context.packageName, R.layout.empty)
        }

        val post = posts[position]
        val image = try {
            GlideApp.with(context)
                .asBitmap()
                .load(post.imageUrl)
                .override(context.resources.getDimensionPixelSize(R.dimen.posts_widget_row_image_dimensions))
                .centerCrop()
                .submit()
                .get(IMAGE_DOWNLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        return RemoteViews(context.packageName, R.layout.posts_widget_row).apply {
            setOnClickFillInIntent(R.id.postsWidgetRowRoot, newPostActivityFillInIntent(post.id))
            setTextViewText(R.id.postsWidgetRowText, post.title)
            if (image != null) {
                setImageViewBitmap(R.id.postsWidgetRowImage, image)
            }
        }
    }

    override fun getCount() = min(posts.size, MAX_ITEMS)

    override fun getViewTypeCount() = 1

    override fun onDestroy() {
    }

}