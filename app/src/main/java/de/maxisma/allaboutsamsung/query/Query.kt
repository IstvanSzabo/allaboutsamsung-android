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
        val onlyTags: List<TagId>?
    ) : Query()
}

fun Query.newExecutor(wordpressApi: WordpressApi, db: Db) = when (this) {
    Query.Empty -> EmptyQueryExecutor(wordpressApi, db)
    is Query.Filter -> FilterQueryExecutor(this, wordpressApi, db)
}

private const val POSTS_PER_PAGE = 20

private class EmptyQueryExecutor(private val wordpressApi: WordpressApi, private val db: Db) : QueryExecutor {

    override val data: LiveData<List<Post>> = db.postDao.posts(oldestThresholdUtc = Date(0))

    override fun tagsForPost(postId: PostId): Deferred<List<Tag>> = async(IOPool) {
        db.postTagDao.tags(postId)
    }

    override fun categoriesForPost(postId: PostId): Deferred<List<Category>> = async(IOPool) {
        db.postCategoryDao.categories(postId)
    }

    override fun tagById(tagId: TagId): Deferred<Tag> = async(IOPool) {
        db.tagDao.tag(tagId)!!
    }

    override fun categoryById(categoryId: CategoryId): Deferred<Category> = async(IOPool) {
        db.categoryDao.category(categoryId)!!
    }

    private suspend fun fetchPosts(beforeGmt: Date? = null) {
        val posts = wordpressApi.posts(page = 1, postsPerPage = POSTS_PER_PAGE, search = null, onlyCategories = null, onlyTags = null, beforeGmt = beforeGmt).await()
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
    }

    override fun requestNewerPosts() = launch(IOPool) {
        fetchPosts()
    }

    override fun requestOlderPosts() = launch(IOPool) {
        fetchPosts(db.postDao.oldestDate())
    }

}

private class FilterQueryExecutor(val query: Query.Filter, private val wordpressApi: WordpressApi, private val db: Db) : QueryExecutor {

    private val postIds = mutableSetOf<PostId>()
    private val _data = MutableLiveData<List<Post>>()

    override val data: LiveData<List<Post>> = _data

    override fun tagById(tagId: TagId): Deferred<Tag> = async(IOPool) {
        db.tagDao.tag(tagId)!!
    }

    override fun categoryById(categoryId: CategoryId): Deferred<Category> = async(IOPool) {
        db.categoryDao.category(categoryId)!!
    }

    override fun tagsForPost(postId: PostId): Deferred<List<Tag>> = async(IOPool) {
        db.postTagDao.tags(postId)
    }

    override fun categoriesForPost(postId: PostId): Deferred<List<Category>> = async(IOPool) {
        db.postCategoryDao.categories(postId)
    }

    private suspend fun fetchPosts(beforeGmt: Date? = null) {
        val posts = wordpressApi.posts(page = 1, postsPerPage = POSTS_PER_PAGE,
            search = query.string, onlyCategories = query.onlyCategories?.let { CategoryIdsDto(it.toSet()) }, onlyTags = query.onlyTags?.let { TagIdsDto(it.toSet()) },
            beforeGmt = beforeGmt
        ).await()
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

        postIds += posts.map { it.id }

        _data.postValue(db.postDao.posts(postIds))
    }

    override fun requestNewerPosts() = launch {
        fetchPosts()
    }

    override fun requestOlderPosts() = launch {
        fetchPosts(beforeGmt = _data.value!!.lastOrNull()?.dateUtc)
    }

}