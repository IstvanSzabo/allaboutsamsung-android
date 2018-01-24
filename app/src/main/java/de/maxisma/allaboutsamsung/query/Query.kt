package de.maxisma.allaboutsamsung.query

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
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
import de.maxisma.allaboutsamsung.rest.CategoryIdsDto
import de.maxisma.allaboutsamsung.rest.PostDto
import de.maxisma.allaboutsamsung.rest.PostIdsDto
import de.maxisma.allaboutsamsung.rest.TagIdsDto
import de.maxisma.allaboutsamsung.rest.UserIdsDto
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.rest.allCategories
import de.maxisma.allaboutsamsung.rest.allTags
import de.maxisma.allaboutsamsung.rest.allUsers
import de.maxisma.allaboutsamsung.utils.IOPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import java.util.Date

interface QueryExecutor {
    val data: LiveData<List<Post>>
    fun tagById(tagId: TagId): Deferred<Tag>
    fun categoryById(categoryId: CategoryId): Deferred<Category>
    fun tagsForPost(postId: PostId): Deferred<List<Tag>>
    fun categoriesForPost(postId: PostId): Deferred<List<Category>>
    fun requestNewerPosts(): Job
    fun requestOlderPosts(): Job
}

sealed class Query {
    object Empty : Query()
    data class Filter(
        val string: String?,
        val onlyCategories: List<CategoryId>?,
        val onlyTags: List<TagId>?,
        val onlyIds: List<PostId>?
    ) : Query()
}

fun Query.newExecutor(wordpressApi: WordpressApi, db: Db, onError: (Exception) -> Unit): QueryExecutor = when (this) {
    Query.Empty -> EmptyQueryExecutor(wordpressApi, db, onError)
    is Query.Filter -> FilterQueryExecutor(this, wordpressApi, db, onError)
}

private const val POSTS_PER_PAGE = 20
private const val TIMEOUT_MS = 30_000L

// TODO Auto-retry
private abstract class DbQueryExecutor(
    private val wordpressApi: WordpressApi,
    private val db: Db,
    private val onError: (Exception) -> Unit
) : QueryExecutor {

    override val data: LiveData<List<Post>> = db.postDao.posts(oldestThresholdUtc = Date(0))

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

    protected abstract suspend fun fetchPosts(beforeGmt: Date?): List<PostDto>

    protected abstract suspend fun oldestPostDateUtc(): Date?

    protected open suspend fun onInsertedPosts(postIds: Set<PostId>) {}

    private suspend fun fetchPostsAndRelated(beforeGmt: Date? = null) = try {
        withTimeout(TIMEOUT_MS) {
            val posts = fetchPosts(beforeGmt)
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
    } catch (e: Exception) {
        onError(e)
    }

    final override fun requestNewerPosts() = launch(IOPool) {
        fetchPostsAndRelated()
    }

    final override fun requestOlderPosts() = launch(IOPool) {
        fetchPostsAndRelated(oldestPostDateUtc())
    }
}

private class EmptyQueryExecutor(
    private val wordpressApi: WordpressApi,
    private val db: Db,
    onError: (Exception) -> Unit
) : DbQueryExecutor(wordpressApi, db, onError) {
    override suspend fun fetchPosts(beforeGmt: Date?) = wordpressApi
        .posts(page = 1, postsPerPage = POSTS_PER_PAGE,
            search = null, onlyCategories = null, onlyTags = null, onlyIds = null, beforeGmt = beforeGmt)
        .await()

    override suspend fun oldestPostDateUtc() = db.postDao.oldestDate()
}

private class FilterQueryExecutor(
    val query: Query.Filter,
    private val wordpressApi: WordpressApi,
    private val db: Db,
    onError: (Exception) -> Unit
) : DbQueryExecutor(wordpressApi, db, onError) {

    private val postIds = mutableSetOf<PostId>()
    private val _data = MutableLiveData<List<Post>>()

    override val data: LiveData<List<Post>> = _data

    override suspend fun fetchPosts(beforeGmt: Date?) = wordpressApi
        .posts(
            page = 1,
            postsPerPage = POSTS_PER_PAGE,
            search = query.string,
            onlyCategories = query.onlyCategories?.let { CategoryIdsDto(it.toSet()) },
            onlyTags = query.onlyTags?.let { TagIdsDto(it.toSet()) },
            onlyIds = query.onlyIds?.let { PostIdsDto(it.toSet()) },
            beforeGmt = beforeGmt
        ).await()

    override suspend fun onInsertedPosts(postIds: Set<PostId>) {
        super.onInsertedPosts(postIds)
        this.postIds += postIds
        _data.postValue(db.postDao.posts(this.postIds))
    }

    override suspend fun oldestPostDateUtc() = _data.value?.lastOrNull()?.dateUtc

}