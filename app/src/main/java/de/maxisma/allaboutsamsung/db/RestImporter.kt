package de.maxisma.allaboutsamsung.db

import de.maxisma.allaboutsamsung.rest.CategoryDto
import de.maxisma.allaboutsamsung.rest.CategoryIdDto
import de.maxisma.allaboutsamsung.rest.PostDto
import de.maxisma.allaboutsamsung.rest.TagDto
import de.maxisma.allaboutsamsung.rest.TagIdDto
import de.maxisma.allaboutsamsung.rest.UserDto
import de.maxisma.allaboutsamsung.rest.UserIdDto
import de.maxisma.allaboutsamsung.utils.htmlUnescape
import org.jsoup.Jsoup

data class MissingMeta(
    val missingCategoryIds: Set<CategoryIdDto>,
    val missingTagIds: Set<TagIdDto>,
    val missingUserIds: Set<UserIdDto>
)

fun Db.findMissingMeta(postDtos: List<PostDto>): MissingMeta {
    val knownCategoryIds = categoryDao.categoryIds().toHashSet()
    val knownTagIds = tagDao.tagIds().toHashSet()
    val knownUserIds = userDao.userIds().toHashSet()
    val missingCategoryIds = postDtos.asSequence().flatMap { it.categories.asSequence() }.toHashSet() - knownCategoryIds
    val missingTagIds = postDtos.asSequence().flatMap { it.tags.asSequence() }.toHashSet() - knownTagIds
    val missingUserIds = postDtos.asSequence().map { it.author }.toHashSet() - knownUserIds
    return MissingMeta(missingCategoryIds, missingTagIds, missingUserIds)
}

fun Db.importCategoryDtos(categoryDtos: List<CategoryDto>) {
    categoryDao.insertCategories(
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
}

fun Db.importTagDtos(tagDtos: List<TagDto>) {
    tagDao.insertTags(
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

fun Db.importPostDtos(postDtos: List<PostDto>) {
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
                imageUrl = postDto.content.rendered.firstImageUrlFromHtml()
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

fun Db.importUserDtos(userDtos: List<UserDto>) {
    userDao.insertUsers(userDtos.map { User(it.id, it.name) })
}

fun String.firstImageUrlFromHtml(): String? = Jsoup.parse(this)
    .body()
    .getElementsByTag("img")
    .first()
    ?.attr("src")
