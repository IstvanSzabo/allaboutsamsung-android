package de.maxisma.allaboutsamsung.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import java.util.Date

typealias CategoryId = Int
typealias TagId = Int
typealias PostId = Long
typealias UserId = Int
typealias PlaylistId = String
typealias VideoId = String

@Entity(
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["author"], onDelete = ForeignKey.RESTRICT)
    ]
)
data class Post(
    @PrimaryKey val id: PostId,
    val dateUtc: Date,
    val slug: String,
    val link: String,
    val title: String,
    val content: String,
    @ColumnInfo(index = true) val author: Int,
    val imageUrl: String?
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

// No foreign keys as the tags have not necessarily been downloaded
@Entity
data class TagSubscription(@PrimaryKey val id: TagId)

// No foreign keys as the categories have not necessarily been downloaded
@Entity
data class CategorySubscription(@PrimaryKey val id: CategoryId)

@Entity
data class User(
    @PrimaryKey val id: UserId,
    val name: String
)

@Entity(
    primaryKeys = ["playlistId", "videoId"],
    foreignKeys = [
        ForeignKey(entity = Video::class, parentColumns = ["id"], childColumns = ["videoId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class PlaylistItem(
    val playlistId: PlaylistId,
    val videoId: VideoId
)

@Entity
data class Video(
    @PrimaryKey val id: VideoId,
    val title: String,
    val thumbnailUrl: String,
    val publishedUtc: Date,
    val expiryDateUtc: Date
)