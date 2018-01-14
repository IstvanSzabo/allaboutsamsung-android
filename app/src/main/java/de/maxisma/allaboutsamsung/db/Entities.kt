package de.maxisma.allaboutsamsung.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import java.util.Date

typealias CategoryId = Int
typealias TagId = Int
typealias PostId = Long

@Entity
data class Post(
        @PrimaryKey val id: PostId,
        val dateUtc: Date,
        val slug: String,
        val link: String,
        val title: String,
        val content: String,
        val author: Int
)

@Entity(
        primaryKeys = ["postId", "categoryId"],
        foreignKeys = [
            ForeignKey(entity = Post::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE),
            ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
        ]
)
data class PostCategory(
        @ColumnInfo(index = true) val postId: PostId,
        @ColumnInfo(index = true) val categoryId: CategoryId
)

@Entity(
        primaryKeys = ["postId", "tagId"],
        foreignKeys = [
            ForeignKey(entity = Post::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE),
            ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
        ]
)
data class PostTag(
        @ColumnInfo(index = true) val postId: PostId,
        @ColumnInfo(index = true) val tagId: TagId
)

@Entity
data class Category(
        @PrimaryKey val id: CategoryId,
        val count: Int,
        val description: String,
        val name: String,
        val slug: String
)

@Entity
data class Tag(
        @PrimaryKey val id: TagId,
        val count: Int,
        val description: String,
        val name: String,
        val slug: String
)