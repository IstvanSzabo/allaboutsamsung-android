package de.maxisma.allaboutsamsung.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters

@Database(entities = [Post::class, Category::class, Tag::class, PostCategory::class, PostTag::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class Db: RoomDatabase() {
    abstract val postDao: PostDao
    abstract val categoryDao: CategoryDao
    abstract val tagDao: TagDao
    abstract val postCategoryDao: PostCategoryDao
    abstract val postTagDao: PostTagDao
    abstract val postMetaDao: PostMetaDao
}