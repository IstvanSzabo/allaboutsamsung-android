package de.maxisma.allaboutsamsung.db

import de.maxisma.allaboutsamsung.R.string.categories
import de.maxisma.allaboutsamsung.rest.CategoryDto
import de.maxisma.allaboutsamsung.rest.CategoryIdDto
import de.maxisma.allaboutsamsung.rest.PostDto
import de.maxisma.allaboutsamsung.rest.TagDto
import de.maxisma.allaboutsamsung.rest.TagIdDto
import de.maxisma.allaboutsamsung.rest.UserDto
import de.maxisma.allaboutsamsung.rest.UserIdDto
import de.maxisma.allaboutsamsung.utils.htmlUnescape
import org.jsoup.Jsoup
import java.util.Date

data class MissingMeta(
    val missingCategoryIds: Set<CategoryIdDto>,
    val missingTagIds: Set<TagIdDto>,
    val missingUserIds: Set<UserIdDto>
)

/**
 * Returns any category, tag and user IDs currently not found in the local DB
 * referenced by the given posts.
 */
suspend fun Db.findMissingMeta(postDtos: List<PostDto>): MissingMeta {
    val knownCategoryIds = categoryDao.categoryIds().toHashSet()
    val knownTagIds = tagDao.tagIds().toHashSet()
    val knownUserIds = userDao.userIds().toHashSet()
    val missingCategoryIds = postDtos.asSequence().flatMap { it.categories.asSequence() }.toHashSet() - knownCategoryIds
    val missingTagIds = postDtos.asSequence().flatMap { it.tags.asSequence() }.toHashSet() - knownTagIds
    val missingUserIds = postDtos.asSequence().map { it.author }.toHashSet() - knownUserIds
    return MissingMeta(missingCategoryIds, missingTagIds, missingUserIds)
}

/**
 * Convert DTOs into DB entities and upsert them.
 *
 * @param deleteAllExcept If true, delete all categories but the ones in the DTO list
 */
suspend fun Db.importCategoryDtos(categoryDtos: List<CategoryDto>, deleteAllExcept: Boolean = false) {
    categoryDao.upsertCategories(
        categoryDtos.map {
            Category(
                id = it.id,
                slug = it.slug,
                count = it.count,
                description = it.description,
                name = it.name.htmlUnescape()
            )
        }
    )
    if (deleteAllExcept) {
        categoryDao.deleteExcept(categoryDtos.map { it.id })
    }
}

/**
 * Convert DTOs into DB entities and upsert them.
 */
suspend fun Db.importTagDtos(tagDtos: List<TagDto>) {
    tagDao.upsertTags(
        tagDtos.map {
            Tag(
                id = it.id,
                slug = it.slug,
                count = it.count,
                description = it.description,
                name = it.name.htmlUnescape()
            )
        }
    )
}

/**
 * Convert DTOs into DB entities and upsert them.
 */
suspend fun Db.importPostDtos(postDtos: List<PostDto>) {
    val importDate = Date()
    val postsWithMeta = postDtos
        .asSequence()
        .map { postDto ->
            val post = Post(
                id = postDto.id,
                author = postDto.author,
                content = postDto.content.rendered,
                dateUtc = postDto.date_gmt,
                link = postDto.link,
                slug = postDto.slug,
                title = postDto.title.rendered.htmlUnescape(),
                imageUrl = postDto.content.rendered.firstImageUrlFromHtml(),
                dbItemCreatedDateUtc = importDate
            )
            val usedCategories = postDto.categories.map { category ->
                PostCategory(postDto.id, category)
            }
            val usedTags = postDto.tags.map { tag ->
                PostTag(postDto.id, tag)
            }
            PostWithMeta(post, usedCategories, usedTags)
        }
    postMetaDao.insertPostsWithMeta(postsWithMeta, postDao, postCategoryDao, postTagDao)
}

/**
 * Convert DTOs into DB entities and upsert them.
 */
suspend fun Db.importUserDtos(userDtos: List<UserDto>) {
    userDao.upsertUsers(userDtos.map { User(it.id, it.name) })
}

/**
 * Finds the URL of the first image in the post HTML.
 */
fun String.firstImageUrlFromHtml(): String? = Jsoup.parse(this)
    .body()
    .getElementsByTag("img")
    .first()
    ?.attr("src")
