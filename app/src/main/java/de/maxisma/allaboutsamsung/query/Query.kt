package de.maxisma.allaboutsamsung.query

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import de.maxisma.allaboutsamsung.db.Category
import de.maxisma.allaboutsamsung.db.CategoryId
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.db.Tag
import de.maxisma.allaboutsamsung.db.TagId
import de.maxisma.allaboutsamsung.db.findMissingMeta
import de.maxisma.allaboutsamsung.db.importCategoryDtos
import de.maxisma.allaboutsamsung.db.importPostDtos
import de.maxisma.allaboutsamsung.db.importTagDtos
import de.maxisma.allaboutsamsung.db.importUserDtos
import de.maxisma.allaboutsamsung.rest.CategoryIdsDto
import de.maxisma.allaboutsamsung.rest.PostDto
import de.maxisma.allaboutsamsung.rest.PostIdsDto
import de.maxisma.allaboutsamsung.rest.TagIdsDto
import de.maxisma.allaboutsamsung.rest.UserIdsDto
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.rest.allCategories
import de.maxisma.allaboutsamsung.rest.allTags
import de.maxisma.allaboutsamsung.rest.allUsers
import de.maxisma.allaboutsamsung.utils.DbWriteDispatcher
import de.maxisma.allaboutsamsung.utils.IOPool
import de.maxisma.allaboutsamsung.utils.SwitchableLiveData
import de.maxisma.allaboutsamsung.utils.observeUntilFalse
import de.maxisma.allaboutsamsung.utils.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.Date

interface QueryExecutor {
    /**
     * The [Query] representing the data of this executor
     */
    val query: Query

    /**
     * Observable list of posts
     */
    val data: LiveData<List<Post>>

    /**
     * Helper method to get the first non-null value of [data]
     * immediately.
     */
    suspend fun dataImmediate(): List<Post>

    suspend fun tagById(tagId: TagId): Tag
    suspend fun categoryById(categoryId: CategoryId): Category
    suspend fun tagsForPost(postId: PostId): List<Tag>
    suspend fun categoriesForPost(postId: PostId): List<Category>

    /**
     * Load the latest posts corresponding to the [query] from the server and import them into the database.
     * In the meantime, show data stored currently in the database, even if it already expired.
     */
    fun requestNewerPosts(): Job

    /**
     * Load posts older than the currently oldest post contained in [data] and import them. As with [requestNewerPosts],
     * even expired data is shown to make the user see posts as soon as possible. To avoid jumping
     * effects while scrolling down, in contrast to [requestNewerPosts] expired now-deleted posts will
     * still be shown when this method completes.
     */
    fun requestOlderPosts(): Job

    /**
     * Load the specified post and import it into the database. Needs to be a post
     * that matches the query.
     */
    fun refresh(postId: PostId): Job
}

sealed class Query {
    /**
     * A query that contains all posts
     */
    object Empty : Query()

    /**
     * A filtered query. [string] refers to a free-text query.
     * All conditions here are connecting using an AND operator.
     */
    data class Filter(
        val string: String? = null,
        val onlyCategories: List<CategoryId>? = null,
        val onlyTags: List<TagId>? = null,
        val onlyIds: List<PostId>? = null
    ) : Query()
}

/**
 * Create a [QueryExecutor] for the given [Query]
 *
 * @param onError Called on any errors that happened while downloading posts using the returned [QueryExecutor]
 */
fun Query.newExecutor(wordpressApi: WordpressApi, db: Db, keyValueStore: KeyValueStore, coroutineScope: CoroutineScope, onError: (Exception) -> Unit): QueryExecutor = when (this) {
    Query.Empty -> EmptyQueryExecutor(wordpressApi, db, keyValueStore, onError, coroutineScope)
    is Query.Filter -> FilterQueryExecutor(this, wordpressApi, keyValueStore, db, onError, coroutineScope)
}

const val WORDPRESS_POSTS_PER_PAGE = 20
private const val POST_INFO_EXPIRY_MS = 15 * 60 * 1000L

private typealias Success = Boolean

private abstract class DbQueryExecutor(
    private val wordpressApi: WordpressApi,
    private val db: Db,
    private val keyValueStore: KeyValueStore,
    private val onError: (Exception) -> Unit,
    coroutineScope: CoroutineScope
) : QueryExecutor, CoroutineScope by coroutineScope {

    protected val oldestAcceptableDataAgeUtc: Date
        get() = Date(System.currentTimeMillis() - POST_INFO_EXPIRY_MS)

    private val nonExpired
        get() = db.postDao.posts(
            oldestThresholdUtc = Date(0),
            latestAcceptableDbItemCreatedDateUtc = oldestAcceptableDataAgeUtc
        )

    // This has a custom getter because otherwise it is not updated while it has no subscriber.
    // If we then subscribe again (because we switch from nonExpired), the LiveData
    // first posts the outdated data (which may not have every item contained by nonExpired,
    // thus causing the RecyclerView to jump) and only then updates and posts again.
    private val includingExpired
        get() = db.postDao.posts(
            oldestThresholdUtc = Date(0),
            latestAcceptableDbItemCreatedDateUtc = Date(0)
        )

    private var displayedData = SwitchableLiveData(nonExpired)

    override val data: LiveData<List<Post>> = displayedData

    final override suspend fun tagsForPost(postId: PostId): List<Tag> = withContext(IOPool) {
        db.postTagDao.tags(postId)
    }

    final override suspend fun categoriesForPost(postId: PostId): List<Category> = withContext(IOPool) {
        db.postCategoryDao.categories(postId)
    }

    final override suspend fun tagById(tagId: TagId): Tag = withContext(IOPool) {
        db.tagDao.tag(tagId)!!
    }

    final override suspend fun categoryById(categoryId: CategoryId): Category = withContext(IOPool) {
        db.categoryDao.category(categoryId)!!
    }

    /**
     * Download posts corresponding to the [query] from the server
     *
     * The two conditions induced by the parameters are connecting using an AND operator.
     *
     * @param beforeGmt Only download posts published before this date
     * @param onlyIds Only download posts having these IDs
     */
    protected abstract suspend fun fetchPosts(beforeGmt: Date?, onlyIds: List<PostId>?): List<PostDto>

    /**
     * Called as soon as the posts with these IDs are imported into the DB,
     * including any metadata.
     */
    protected open suspend fun onInsertedPosts(postIds: Set<PostId>) {}

    private suspend fun fetchPostsAndRelated(beforeGmt: Date? = null, onlyIds: List<PostId>? = null): Success = try {
        retry(
            HttpException::class,
            JsonDataException::class,
            JsonEncodingException::class,
            IOException::class,
            TimeoutCancellationException::class
        ) {
            val posts = fetchPosts(beforeGmt, onlyIds)
            val (missingCategoryIds, missingTagIds, missingUserIds) = db.findMissingMeta(posts)
            val categories = if (missingCategoryIds.isEmpty()) {
                emptyList()
            } else {
                // onlyIds = null to fetch *all* categories from the server.
                // We need them anyway for CategoryActivity
                wordpressApi.allCategories(onlyIds = null)
            }
            val tags = if (missingTagIds.isEmpty()) {
                emptyList()
            } else {
                wordpressApi.allTags(TagIdsDto(missingTagIds))
            }
            val users = if (missingUserIds.isEmpty()) {
                emptyList()
            } else {
                wordpressApi.allUsers(UserIdsDto(missingUserIds))
            }

            keyValueStore.lastCategoryRefreshMs = System.currentTimeMillis()
            db.importCategoryDtos(categories)
            db.importTagDtos(tags)
            db.importUserDtos(users)
            db.importPostDtos(posts)

            onInsertedPosts(posts.map { it.id }.toSet())
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        onError(e)
        false
    }

    /**
     * Switch to data from the DB that even contains expired entries.
     * Call [updater] with oldest date that was contained in [data] *before* switching to expired entries.
     * If [hideExpiredAfterUpdate] is true, we afterwards switch back to non-expired data only.
     */
    private suspend fun showExpiredThenUpdate(hideExpiredAfterUpdate: Boolean = false, updater: suspend (Date?) -> Success) {
        val oldestDateOnScreen = data.value?.minBy { it.dateUtc }?.dateUtc

        // Show the user some stale data until we got new data
        displayedData.delegate = includingExpired

        val success = updater(oldestDateOnScreen)
        if (success && hideExpiredAfterUpdate) {
            // Switch back to non-expired data to make the RecyclerView request more rows
            // when scrolling down.
            displayedData.delegate = nonExpired
        }
    }

    final override fun requestNewerPosts() = launch(DbWriteDispatcher) {
        showExpiredThenUpdate(hideExpiredAfterUpdate = true) {
            fetchPostsAndRelated()
        }
    }

    final override fun requestOlderPosts() = launch(DbWriteDispatcher) {
        // Here we don't delete expired posts, because this can lead to jumping effects
        // in the RecyclerView. Expired data is only cleared when new posts are
        // successfully fetched in requestNewerPosts().
        showExpiredThenUpdate { oldestDateOnScreen ->
            fetchPostsAndRelated(oldestDateOnScreen)
        }
    }

    final override fun refresh(postId: PostId) = launch(DbWriteDispatcher) {
        fetchPostsAndRelated(onlyIds = listOf(postId))
    }

    final override suspend fun dataImmediate(): List<Post> {
        val mutex = Mutex(locked = true)
        lateinit var currentData: List<Post>
        data.observeUntilFalse {
            currentData = it ?: return@observeUntilFalse true
            mutex.unlock()
            false
        }
        mutex.lock()
        return currentData
    }
}

private class EmptyQueryExecutor(
    private val wordpressApi: WordpressApi,
    db: Db,
    keyValueStore: KeyValueStore,
    onError: (Exception) -> Unit,
    coroutineScope: CoroutineScope
) : DbQueryExecutor(wordpressApi, db, keyValueStore, onError, coroutineScope) {
    override val query = Query.Empty
    override suspend fun fetchPosts(beforeGmt: Date?, onlyIds: List<PostId>?) = wordpressApi
        .postsAsync(
            page = 1, postsPerPage = WORDPRESS_POSTS_PER_PAGE,
            search = null, onlyCategories = null, onlyTags = null,
            onlyIds = onlyIds?.let { PostIdsDto(it.toSet()) }, beforeGmt = beforeGmt
        )
        .await()

}

private class FilterQueryExecutor(
    override val query: Query.Filter,
    private val wordpressApi: WordpressApi,
    keyValueStore: KeyValueStore,
    private val db: Db,
    onError: (Exception) -> Unit,
    coroutineScope: CoroutineScope
) : DbQueryExecutor(wordpressApi, db, keyValueStore, onError, coroutineScope) {

    private val postIds = mutableSetOf<PostId>()
    private val _data = MutableLiveData<List<Post>>()

    override val data: LiveData<List<Post>> = _data

    override suspend fun fetchPosts(beforeGmt: Date?, onlyIds: List<PostId>?) = wordpressApi
        .postsAsync(
            page = 1,
            postsPerPage = WORDPRESS_POSTS_PER_PAGE,
            search = query.string,
            onlyCategories = query.onlyCategories?.let { CategoryIdsDto(it.toSet()) },
            onlyTags = query.onlyTags?.let { TagIdsDto(it.toSet()) },
            onlyIds = (onlyIds ?: query.onlyIds)?.let { PostIdsDto(it.toSet()) },
            beforeGmt = beforeGmt
        ).await()

    override suspend fun onInsertedPosts(postIds: Set<PostId>) {
        super.onInsertedPosts(postIds)
        this.postIds += postIds
        _data.postValue(db.postDao.posts(this.postIds))
    }

}