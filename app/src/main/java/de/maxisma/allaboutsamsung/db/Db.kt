package de.maxisma.allaboutsamsung.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Post::class, Category::class, Tag::class, PostCategory::class, PostTag::class,
        User::class, CategorySubscription::class, TagSubscription::class, Video::class,
        PlaylistItem::class, SeenVideo::class
    ], version = 1, exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class Db : RoomDatabase() {
    abstract val postDao: PostDao
    abstract val categoryDao: CategoryDao
    abstract val tagDao: TagDao
    abstract val postCategoryDao: PostCategoryDao
    abstract val postTagDao: PostTagDao
    abstract val postMetaDao: PostMetaDao
    abstract val userDao: UserDao
    abstract val videoDao: VideoDao
}