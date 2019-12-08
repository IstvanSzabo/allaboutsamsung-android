package de.maxisma.allaboutsamsung.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.Update
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.utils.Iso8601Utils
import java.util.Date

/**
 * Emulates upsert. Needed because INSERT OR REPLACE causes row deletion -> foreign key constraint violation.
 *
 * @param insert A function that inserts the content.
 * @param update A function that updates the content.
 */
private inline fun <T> buildUpserter(
    crossinline insert: suspend (T) -> Unit,
    crossinline update: suspend (T) -> Unit
): suspend (T) -> Unit = { content: T ->
    insert(content)
    update(content)
}

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
    open suspend fun insertPostsWithMeta(postsWithMeta: Sequence<PostWithMeta>, postDao: PostDao, postCategoryDao: PostCategoryDao, postTagDao: PostTagDao) {
        for ((post, categories, tags) in postsWithMeta) {
            require(categories.all { it.postId == post.id })
            require(tags.all { it.postId == post.id })

            postDao.upsertPost(post)
            postCategoryDao.replacePostCategories(post.id, categories)
            postTagDao.replacePostTags(post.id, tags)
        }
    }

    @Query("SELECT Post.*, User.name AS authorName FROM Post JOIN User ON Post.author = User.id WHERE Post.id = :postId AND User.id IS NOT NULL")
    abstract fun postWithAuthorName(postId: PostId): LiveData<PostWithAuthorName>
}

@Dao
abstract class PostDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPost(post: Post)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun updatePost(post: Post)

    private val upserter = buildUpserter(::insertPost, ::updatePost)

    suspend fun upsertPost(post: Post) = upserter(post)

    @Query("DELETE FROM Post WHERE dbItemCreatedDateUtc < datetime(:oldestAllowedDate)")
    abstract suspend fun deleteExpired(oldestAllowedDate: Date)

    /**
     * Posts that were published before [oldestThresholdUtc] and were put into the cache not later than
     * [latestAcceptableDbItemCreatedDateUtc]. Any posts that were published before these expired posts
     * are also filtered out.
     */
    @Query(
        """
        SELECT * FROM Post
        WHERE datetime(dateUtc) >= datetime(:oldestThresholdUtc)
        AND datetime(dbItemCreatedDateUtc) >= datetime(:latestAcceptableDbItemCreatedDateUtc)
        AND datetime(dateUtc) >= ifnull((SELECT min(datetime(dateUtc)) FROM Post WHERE datetime(dbItemCreatedDateUtc) < datetime(:latestAcceptableDbItemCreatedDateUtc)), datetime(0))
        ORDER BY datetime(dateUtc) DESC
        """
    )
    abstract fun posts(oldestThresholdUtc: Date, latestAcceptableDbItemCreatedDateUtc: Date): LiveData<List<Post>>

    @Query("SELECT * FROM Post WHERE id IN (:ids) ORDER BY datetime(dateUtc) DESC")
    abstract suspend fun posts(ids: Set<PostId>): List<Post>

    @Query("SELECT * FROM Post WHERE id = :id")
    abstract fun post(id: PostId): LiveData<Post>

    @Query(
        """
        SELECT min(dateUtc) FROM Post
        WHERE datetime(dbItemCreatedDateUtc) >= datetime(:latestAcceptableDbItemCreatedDateUtc)
        AND datetime(dateUtc) >= ifnull((SELECT min(datetime(dateUtc)) FROM Post WHERE datetime(dbItemCreatedDateUtc) < datetime(:latestAcceptableDbItemCreatedDateUtc)), datetime(0))
    """
    )
    abstract suspend fun oldestNonExpiredDate(latestAcceptableDbItemCreatedDateUtc: Date): Date?

    /**
     * A recent post classified as breaking news, if any.
     */
    @Query(
        """
        SELECT Post.* FROM Post
        JOIN PostCategory ON Post.id = PostCategory.postId
        WHERE categoryId = ${BuildConfig.BREAKING_CATEGORY_ID} AND dateTime(dateUtc, '+1 day') >= datetime('now')
        ORDER BY datetime(dateUtc) DESC
        LIMIT 1
        """
    )
    abstract fun latestActiveBreakingPost(): LiveData<Post?>
}

@Dao
abstract class CategoryDao {
    @Query("DELETE FROM Category WHERE id NOT IN (:categoryIds)")
    abstract suspend fun deleteExcept(categoryIds: List<CategoryId>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertCategories(categories: List<Category>)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun updateCategories(categories: List<Category>)

    private val categoriesUpserter = buildUpserter(::insertCategories, ::updateCategories)

    suspend fun upsertCategories(categories: List<Category>) = categoriesUpserter(categories)

    @Query("SELECT id FROM Category")
    abstract suspend fun categoryIds(): List<CategoryId>

    @Query("SELECT * FROM Category WHERE id = :categoryId")
    abstract suspend fun category(categoryId: CategoryId): Category?

    @Query("SELECT * FROM Category")
    abstract fun categories(): LiveData<List<Category>>

    @Query("SELECT * FROM Category WHERE id IN (:categoryIds)")
    abstract suspend fun categories(categoryIds: List<CategoryId>): List<Category>

    @Query("SELECT * FROM Category WHERE Category.id IN (SELECT CategorySubscription.id FROM CategorySubscription)")
    abstract suspend fun subscribedCategories(): List<Category>

    @Query("DELETE FROM CategorySubscription")
    abstract suspend fun deleteCategorySubscriptions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategorySubscriptions(categorySubscriptions: List<CategorySubscription>)

    @Transaction
    open suspend fun replaceCategorySubscriptions(categorySubscriptions: List<CategorySubscription>) {
        deleteCategorySubscriptions()
        insertCategorySubscriptions(categorySubscriptions)
    }
}

@Dao
abstract class TagDao {
    @Query("DELETE FROM Tag WHERE id NOT IN (:tagIds)")
    abstract suspend fun deleteExcept(tagIds: List<TagId>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTags(tags: List<Tag>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun updateTags(tags: List<Tag>)

    private val tagUpserter = buildUpserter(::insertTags, ::updateTags)

    suspend fun upsertTags(tags: List<Tag>) = tagUpserter(tags)

    @Query("SELECT id FROM Tag")
    abstract suspend fun tagIds(): List<TagId>

    @Transaction
    open suspend fun replaceAll(tags: List<Tag>) {
        deleteExcept(tags.map { it.id })
        insertTags(tags)
    }

    @Query("SELECT * FROM Tag WHERE id = :tagId")
    abstract suspend fun tag(tagId: TagId): Tag?

    @Query("SELECT * FROM Tag WHERE id IN (:tagIds)")
    abstract suspend fun tags(tagIds: List<TagId>): List<Tag>

    @Query("SELECT * FROM Tag WHERE Tag.id IN (SELECT TagSubscription.id FROM TagSubscription)")
    abstract suspend fun subscribedTags(): List<Tag>

    @Query("DELETE FROM TagSubscription")
    abstract suspend fun deleteTagSubscriptions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTagSubscriptions(tagSubscriptions: List<TagSubscription>)

    @Transaction
    open suspend fun replaceTagSubscriptions(tagSubscriptions: List<TagSubscription>) {
        deleteTagSubscriptions()
        insertTagSubscriptions(tagSubscriptions)
    }
}

@Dao
abstract class PostCategoryDao {
    @Query("DELETE FROM PostCategory WHERE postId = :postId AND categoryId NOT IN (:categoryIds)")
    abstract suspend fun deleteForPostExcept(postId: PostId, categoryIds: List<CategoryId>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPostCategories(postCategories: List<PostCategory>)

    @Transaction
    open suspend fun replacePostCategories(postId: PostId, categories: List<PostCategory>) {
        deleteForPostExcept(postId, categories.map { it.categoryId })
        insertPostCategories(categories)
    }

    /**
     * Categories of the given post.
     */
    @Query("SELECT * FROM Category LEFT JOIN PostCategory ON Category.id = PostCategory.categoryId WHERE PostCategory.postId = :postId")
    abstract suspend fun categories(postId: PostId): List<Category>
}

@Dao
abstract class PostTagDao {
    @Query("DELETE FROM PostTag WHERE postId = :postId AND tagId NOT IN (:tagIds)")
    abstract suspend fun deleteForPostExcept(postId: PostId, tagIds: List<TagId>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPostTags(postTags: List<PostTag>)

    @Transaction
    open suspend fun replacePostTags(postId: PostId, tags: List<PostTag>) {
        deleteForPostExcept(postId, tags.map { it.tagId })
        insertPostTags(tags)
    }

    /**
     * Tags for the given post.
     */
    @Query("SELECT * FROM Tag LEFT JOIN PostTag ON Tag.id = PostTag.tagId WHERE PostTag.postId = :postId")
    abstract suspend fun tags(postId: PostId): List<Tag>
}

@Dao
abstract class UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertUsers(users: List<User>)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun updateUsers(users: List<User>)

    private val upserter = buildUpserter(::insertUsers, ::updateUsers)

    suspend fun upsertUsers(users: List<User>) = upserter(users)

    @Query("SELECT * FROM User WHERE id = :id")
    abstract suspend fun user(id: UserId): User

    @Query("SELECT id FROM User")
    abstract suspend fun userIds(): List<UserId>
}

@Dao
abstract class VideoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertVideos(videos: List<Video>)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun updateVideos(videos: List<Video>)

    private val videoUpserter = buildUpserter(::insertVideos, ::updateVideos)

    suspend fun upsertVideos(videos: List<Video>) = videoUpserter(videos)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPlaylistItems(playlistItems: List<PlaylistItem>)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun updatePlaylistItems(playlistItems: List<PlaylistItem>)

    private val playlistItemUpserter = buildUpserter(::insertPlaylistItems, ::updatePlaylistItems)

    suspend fun upsertPlaylistItems(playlistItems: List<PlaylistItem>) = playlistItemUpserter(playlistItems)

    /**
     * Delete expired videos and videos published before that to avoid inconsistent data.
     */
    @Query(
        """
        DELETE FROM Video
        WHERE datetime(expiryDateUtc) < datetime('now')
        OR datetime(publishedUtc) < (SELECT min(datetime(publishedUtc)) FROM Video WHERE datetime(expiryDateUtc) < datetime('now'))
    """
    )
    abstract suspend fun deleteExpired()

    @Query("SELECT publishedUtc FROM PlaylistItem JOIN Video ON Video.id = PlaylistItem.videoId WHERE playlistId = :playlistId ORDER BY datetime(publishedUtc) ASC LIMIT 1")
    abstract suspend fun oldestDateInPlaylist(playlistId: PlaylistId): Date

    @Query("SELECT Video.* FROM PlaylistItem JOIN Video ON Video.id = PlaylistItem.videoId WHERE playlistId = :playlistId ORDER BY datetime(publishedUtc) DESC")
    abstract fun videosInPlaylist(playlistId: PlaylistId): LiveData<List<Video>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertSeenVideos(seenVideos: List<SeenVideo>)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun updateSeenVideos(seenVideos: List<SeenVideo>)

    private val seenVideosUpserter = buildUpserter(::insertSeenVideos, ::updateSeenVideos)

    suspend fun upsertSeenVideos(seenVideos: List<SeenVideo>) = seenVideosUpserter(seenVideos)

    @Query("SELECT * FROM SeenVideo")
    abstract suspend fun seenVideos(): List<SeenVideo>
}

object DateConverter {
    @TypeConverter
    @JvmStatic
    fun toDate(value: String?): Date? = value?.let { Iso8601Utils.parse(it) }

    @TypeConverter
    @JvmStatic
    fun fromDate(date: Date?): String? = date?.let { Iso8601Utils.format(it) }
}