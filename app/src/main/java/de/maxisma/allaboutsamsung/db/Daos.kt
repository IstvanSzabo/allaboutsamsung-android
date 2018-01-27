package de.maxisma.allaboutsamsung.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Embedded
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

data class PostWithAuthorName(
    @Embedded val post: Post,
    val authorName: String
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

    @Query("SELECT Post.*, User.name AS authorName FROM Post JOIN User ON Post.author = User.id WHERE Post.id = :postId AND User.id IS NOT NULL")
    abstract fun postWithAuthorName(postId: PostId): LiveData<PostWithAuthorName>
}

private const val MAX_POSTS_IN_DB = 100

@Dao
abstract class PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPost(post: Post)

    @Query("SELECT * FROM Post WHERE dateUtc >= datetime(:oldestThresholdUtc) ORDER BY datetime(dateUtc) DESC")
    abstract fun posts(oldestThresholdUtc: Date): LiveData<List<Post>>

    @Query("SELECT * FROM Post WHERE id IN (:ids) ORDER BY datetime(dateUtc) DESC")
    abstract fun posts(ids: Set<PostId>): List<Post>

    @Query("SELECT * FROM Post WHERE id = :id")
    abstract fun post(id: PostId): LiveData<Post>

    @Query("SELECT dateUtc FROM Post ORDER BY datetime(dateUtc) ASC LIMIT 1")
    abstract fun oldestDate(): Date?

    // AS vtable should force SQlite to create a virtual table instead of querying for each row
    @Query("DELETE FROM Post WHERE id NOT IN (SELECT id FROM (SELECT id FROM Post ORDER BY datetime(dateUtc) DESC LIMIT $MAX_POSTS_IN_DB) AS vtable)")
    abstract fun deleteOld()
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

    @Query("SELECT * FROM Category")
    abstract fun categories(): LiveData<List<Category>>

    @Query("SELECT * FROM Category WHERE id IN (:categoryIds)")
    abstract fun categories(categoryIds: List<CategoryId>): List<Category>

    @Query("SELECT * FROM Category WHERE Category.id IN (SELECT CategorySubscription.id FROM CategorySubscription)")
    abstract fun subscribedCategories(): List<Category>

    @Query("DELETE FROM CategorySubscription")
    abstract fun deleteCategorySubscriptions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertCategorySubscriptions(categorySubscriptions: List<CategorySubscription>)

    @Transaction
    open fun replaceCategorySubscriptions(categorySubscriptions: List<CategorySubscription>) {
        deleteCategorySubscriptions()
        insertCategorySubscriptions(categorySubscriptions)
    }
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

    @Query("SELECT * FROM Tag WHERE Tag.id IN (SELECT TagSubscription.id FROM TagSubscription)")
    abstract fun subscribedTags(): List<Tag>

    @Query("DELETE FROM TagSubscription")
    abstract fun deleteTagSubscriptions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertTagSubscriptions(tagSubscriptions: List<TagSubscription>)

    @Transaction
    open fun replaceTagSubscriptions(tagSubscriptions: List<TagSubscription>) {
        deleteTagSubscriptions()
        insertTagSubscriptions(tagSubscriptions)
    }
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

@Dao
abstract class UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUsers(users: List<User>)

    @Query("SELECT * FROM User WHERE id = :id")
    abstract fun user(id: UserId): User

    @Query("SELECT id FROM User")
    abstract fun userIds(): List<UserId>
}

@Dao
abstract class VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertVideos(videos: List<Video>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPlaylistItems(playlistItems: List<PlaylistItem>)

    @Query("""
        DELETE FROM Video
        WHERE datetime(expiryDateUtc) < datetime('now')
        OR datetime(publishedUtc) < (SELECT min(datetime(publishedUtc)) FROM Video WHERE datetime(expiryDateUtc) < datetime('now'))
    """)
    abstract fun deleteExpired()

    @Query("SELECT publishedUtc FROM PlaylistItem JOIN Video ON Video.id = PlaylistItem.videoId WHERE playlistId = :playlistId ORDER BY datetime(publishedUtc) ASC LIMIT 1")
    abstract fun oldestDateInPlaylist(playlistId: PlaylistId): Date

    @Query("SELECT Video.* FROM PlaylistItem JOIN Video ON Video.id = PlaylistItem.videoId WHERE playlistId = :playlistId ORDER BY datetime(publishedUtc) DESC")
    abstract fun videosInPlaylist(playlistId: PlaylistId): LiveData<List<Video>>
}

object DateConverter {
    @TypeConverter
    @JvmStatic
    fun toDate(value: String?): Date? = value?.let { Iso8601Utils.parse(it) }

    @TypeConverter
    @JvmStatic
    fun fromDate(date: Date?): String? = date?.let { Iso8601Utils.format(it) }
}