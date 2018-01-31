package de.maxisma.allaboutsamsung.query

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import de.maxisma.allaboutsamsung.db.Category
import de.maxisma.allaboutsamsung.db.CategoryId
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.db.Tag
import de.maxisma.allaboutsamsung.db.TagId
import de.maxisma.allaboutsamsung.db.findMissingMeta
import de.maxisma.allaboutsamsung.db.importCategoryDtos
import de.maxisma.allaboutsamsung.db.importPostDtos
import de.maxisma.allaboutsamsung.db.importTagDtos
import de.maxisma.allaboutsamsung.db.importUserDtos
import de.maxisma.allaboutsamsung.db.inTransaction
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
import de.maxisma.allaboutsamsung.utils.retry
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import retrofit2.HttpException
import java.io.IOException
import java.util.Date
import kotlin.math.max

interface QueryExecutor {
    val query: Query
    val data: LiveData<List<Post>>

    fun tagById(tagId: TagId): Deferred<Tag>
    fun categoryById(categoryId: CategoryId): Deferred<Category>
    fun tagsForPost(postId: PostId): Deferred<List<Tag>>
    fun categoriesForPost(postId: PostId): Deferred<List<Category>>
    fun requestNewerPosts(): Job
    fun requestOlderPosts(): Job
    fun refresh(postId: PostId): Job
}

sealed class Query {
    object Empty : Query()
    data class Filter(
        val string: String? = null,
        val onlyCategories: List<CategoryId>? = null,
        val onlyTags: List<TagId>? = null,
        val onlyIds: List<PostId>? = null
    ) : Query()
}

fun Query.newExecutor(wordpressApi: WordpressApi, db: Db, onError: (Exception) -> Unit): QueryExecutor = when (this) {
    Query.Empty -> EmptyQueryExecutor(wordpressApi, db, onError)
    is Query.Filter -> FilterQueryExecutor(this, wordpressApi, db, onError)
}

const val WORDPRESS_POSTS_PER_PAGE = 20
private const val TIMEOUT_MS = 30_000L
private const val POST_INFO_EXPIRY_MS = 15 * 60 * 1000L

private typealias Success = Boolean

private abstract class DbQueryExecutor(
    private val wordpressApi: WordpressApi,
    private val db: Db,
    private val onError: (Exception) -> Unit
) : QueryExecutor {

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

    final override fun tagsForPost(postId: PostId): Deferred<List<Tag>> = async(IOPool) {
        db.postTagDao.tags(postId)
    }

    final override fun categoriesForPost(postId: PostId): Deferred<List<Category>> = async(IOPool) {
        db.postCategoryDao.categories(postId)
    }

    final override fun tagById(tagId: TagId): Deferred<Tag> = async(IOPool) {
        db.tagDao.tag(tagId)!!
    }

    final override fun categoryById(categoryId: CategoryId): Deferred<Category> = async(IOPool) {
        db.categoryDao.category(categoryId)!!
    }

    protected abstract suspend fun fetchPosts(beforeGmt: Date?, onlyIds: List<PostId>?): List<PostDto>

    protected abstract suspend fun oldestNonExpiredPostDateUtc(): Date?

    protected open suspend fun onInsertedPosts(postIds: Set<PostId>) {}

    private suspend fun fetchPostsAndRelated(beforeGmt: Date? = null, onlyIds: List<PostId>? = null) = try {
        retry(
            HttpException::class,
            JsonDataException::class,
            JsonEncodingException::class,
            IOException::class,
            TimeoutCancellationException::class
        ) {
            withTimeout(TIMEOUT_MS) {
                val posts = fetchPosts(beforeGmt, onlyIds)
                val (missingCategoryIds, missingTagIds, missingUserIds) = db.findMissingMeta(posts)
                val categories = if (missingCategoryIds.isEmpty()) {
                    emptyList()
                } else {
                    wordpressApi.allCategories(CategoryIdsDto(missingCategoryIds)).await()
                }
                val tags = if (missingTagIds.isEmpty()) {
                    emptyList()
                } else {
                    wordpressApi.allTags(TagIdsDto(missingTagIds)).await()
                }
                val users = if (missingUserIds.isEmpty()) {
                    emptyList()
                } else {
                    wordpressApi.allUsers(UserIdsDto(missingUserIds)).await()
                }
                db.importCategoryDtos(categories)
                db.importTagDtos(tags)
                db.importUserDtos(users)
                db.importPostDtos(posts)

                onInsertedPosts(posts.map { it.id }.toSet())
            }
        }
        true
    } catch (e: Exception) {
        onError(e)
        false
    }

    private suspend fun showExpiredThenUpdate(deleteAllExpired: Boolean = false, updater: suspend (Date) -> Success) {
        // Save it here so we don't delete data later that has just been fetched because an item above expired
        val oldestAcceptableDataAgeUtc = oldestAcceptableDataAgeUtc
        // Show the user some stale data until we got new data
        displayedData.delegate = includingExpired

        val (countBefore, success, countAfter) = db.postDao.inTransaction {
            val countBefore = count()
            val success = updater(oldestAcceptableDataAgeUtc)
            val countAfter = count()

            Triple(countBefore, success, countAfter)
        }

        if (success) {
            // Switch back to non-expired data to make the RecyclerView request more rows
            // when scrolling down.
            displayedData.delegate = nonExpired
            // We delete up to countAfter - countBefore expired rows to try and only delete those that are not contained
            // in the result from the updater and thus have been deleted in WordPress. If no post was deleted,
            // then the difference between these variables will be 0.
            val maxRowsToDelete = if (deleteAllExpired) Int.MAX_VALUE else max(0, countAfter - countBefore)
            db.postDao.deleteExpired(oldestAcceptableDataAgeUtc, maxRowsToDelete)
        }
    }

    final override fun requestNewerPosts() = launch(DbWriteDispatcher) {
        showExpiredThenUpdate(deleteAllExpired = true) {
            fetchPostsAndRelated()
        }
    }

    final override fun requestOlderPosts() = launch(DbWriteDispatcher) {
        showExpiredThenUpdate { oldestAcceptableDataAgeUtc ->
            val oldSize = displayedData.value?.size ?: 0
            var newNonExpiredSize = -1

            while (newNonExpiredSize <= oldSize) {
                val success = fetchPostsAndRelated(oldestNonExpiredPostDateUtc())
                val nextNewNonExpiredSize = db.postDao.countNonExpired(oldestAcceptableDataAgeUtc)
                if (!success) return@showExpiredThenUpdate false
                if (newNonExpiredSize == nextNewNonExpiredSize) return@showExpiredThenUpdate true

                newNonExpiredSize = nextNewNonExpiredSize
            }

            true
        }
    }

    final override fun refresh(postId: PostId) = launch(DbWriteDispatcher) {
        fetchPostsAndRelated(onlyIds = listOf(postId))
    }
}

private class EmptyQueryExecutor(
    private val wordpressApi: WordpressApi,
    private val db: Db,
    onError: (Exception) -> Unit
) : DbQueryExecutor(wordpressApi, db, onError) {
    override val query = Query.Empty
    override suspend fun fetchPosts(beforeGmt: Date?, onlyIds: List<PostId>?) = wordpressApi
        .posts(
            page = 1, postsPerPage = WORDPRESS_POSTS_PER_PAGE,
            search = null, onlyCategories = null, onlyTags = null,
            onlyIds = onlyIds?.let { PostIdsDto(it.toSet()) }, beforeGmt = beforeGmt
        )
        .await()

    override suspend fun oldestNonExpiredPostDateUtc() = db.postDao.oldestNonExpiredDate(oldestAcceptableDataAgeUtc)
}

private class FilterQueryExecutor(
    override val query: Query.Filter,
    private val wordpressApi: WordpressApi,
    private val db: Db,
    onError: (Exception) -> Unit
) : DbQueryExecutor(wordpressApi, db, onError) {

    private val postIds = mutableSetOf<PostId>()
    private val _data = MutableLiveData<List<Post>>()

    override val data: LiveData<List<Post>> = _data

    override suspend fun fetchPosts(beforeGmt: Date?, onlyIds: List<PostId>?) = wordpressApi
        .posts(
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

    override suspend fun oldestNonExpiredPostDateUtc() = _data.value?.lastOrNull()?.dateUtc

}