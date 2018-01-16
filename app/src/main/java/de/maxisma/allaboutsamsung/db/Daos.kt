package de.maxisma.allaboutsamsung.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import android.arch.persistence.room.TypeConverter
import de.maxisma.allaboutsamsung.utils.Iso8601Utils
import java.util.Date

data class PostWithMeta(
        val post: Post,
        val postCategories: List<PostCategory>,
        val postTags: List<PostTag>
)

@Dao
abstract class PostMetaDao {
    @Transaction
    open fun insertPostsWithMeta(postsWithMeta: Sequence<PostWithMeta>, postDao: PostDao, postCategoryDao: PostCategoryDao, postTagDao: PostTagDao) {
        for ((post, categories, tags) in postsWithMeta) {
            require(categories.all { it.postId == post.id })
            require(tags.all { it.postId == post.id })

            postDao.insertPost(post)
            postCategoryDao.replacePostCategories(post.id, categories)
            postTagDao.replacePostTags(post.id, tags)
        }
    }
}

@Dao
abstract class PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPost(post: Post)

    @Query("SELECT * FROM Post WHERE dateUtc >= datetime(:oldestThresholdUtc) ORDER BY datetime(dateUtc) DESC")
    abstract fun posts(oldestThresholdUtc: Date): LiveData<List<Post>>

    @Query("SELECT * FROM Post WHERE id IN (:ids) ORDER BY datetime(dateUtc) DESC")
    abstract fun posts(ids: Set<PostId>): List<Post>

    @Query("SELECT dateUtc FROM Post ORDER BY datetime(dateUtc) ASC LIMIT 1")
    abstract fun oldestDate(): Date
}

@Dao
abstract class CategoryDao {
    @Query("DELETE FROM Category WHERE id NOT IN (:categoryIds)")
    abstract fun deleteExcept(categoryIds: List<CategoryId>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertCategories(categories: List<Category>)

    @Query("SELECT id FROM Category")
    abstract fun categoryIds(): List<CategoryId>

    @Transaction
    open fun replaceAll(categories: List<Category>) {
        deleteExcept(categories.map { it.id })
        insertCategories(categories)
    }

    @Query("SELECT * FROM Category WHERE id = :categoryId")
    abstract fun category(categoryId: CategoryId): Category?

    @Query("SELECT * FROM Category WHERE id IN (:categoryIds)")
    abstract fun categories(categoryIds: List<CategoryId>): List<Category>
}

@Dao
abstract class TagDao {
    @Query("DELETE FROM Tag WHERE id NOT IN (:tagIds)")
    abstract fun deleteExcept(tagIds: List<TagId>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertTags(tags: List<Tag>)

    @Query("SELECT id FROM Tag")
    abstract fun tagIds(): List<TagId>

    @Transaction
    open fun replaceAll(tags: List<Tag>) {
        deleteExcept(tags.map { it.id })
        insertTags(tags)
    }

    @Query("SELECT * FROM Tag WHERE id = :tagId")
    abstract fun tag(tagId: TagId): Tag?

    @Query("SELECT * FROM Tag WHERE id IN (:tagIds)")
    abstract fun tags(tagIds: List<TagId>): List<Tag>
}

@Dao
abstract class PostCategoryDao {
    @Query("DELETE FROM PostCategory WHERE postId = :postId AND categoryId NOT IN (:categoryIds)")
    abstract fun deleteForPostExcept(postId: PostId, categoryIds: List<CategoryId>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPostCategories(postCategories: List<PostCategory>)

    @Transaction
    open fun replacePostCategories(postId: PostId, categories: List<PostCategory>) {
        deleteForPostExcept(postId, categories.map { it.categoryId })
        insertPostCategories(categories)
    }

    @Query("SELECT * FROM Category LEFT JOIN PostCategory ON Category.id = PostCategory.categoryId WHERE PostCategory.postId = :postId")
    abstract fun categories(postId: PostId): List<Category>
}

@Dao
abstract class PostTagDao {
    @Query("DELETE FROM PostTag WHERE postId = :postId AND tagId NOT IN (:tagIds)")
    abstract fun deleteForPostExcept(postId: PostId, tagIds: List<TagId>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPostTags(postTags: List<PostTag>)

    @Transaction
    open fun replacePostTags(postId: PostId, tags: List<PostTag>) {
        deleteForPostExcept(postId, tags.map { it.tagId })
        insertPostTags(tags)
    }

    @Query("SELECT * FROM Tag LEFT JOIN PostTag ON Tag.id = PostTag.tagId WHERE PostTag.postId = :postId")
    abstract fun tags(postId: PostId): List<Tag>
}

object DateConverter {
    @TypeConverter
    @JvmStatic
    fun toDate(value: String?): Date? = value?.let { Iso8601Utils.parse(it) }

    @TypeConverter
    @JvmStatic
    fun fromDate(date: Date?): String? = date?.let { Iso8601Utils.format(it) }
}