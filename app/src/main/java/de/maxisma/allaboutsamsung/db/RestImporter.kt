package de.maxisma.allaboutsamsung.db

import de.maxisma.allaboutsamsung.rest.CategoryDto
import de.maxisma.allaboutsamsung.rest.CategoryIdDto
import de.maxisma.allaboutsamsung.rest.PostDto
import de.maxisma.allaboutsamsung.rest.TagDto
import de.maxisma.allaboutsamsung.rest.TagIdDto

data class MissingMeta(val missingCategoryIds: Set<CategoryIdDto>, val missingTagIds: Set<TagIdDto>)

fun Db.findMissingMeta(postDtos: List<PostDto>): MissingMeta {
    val knownCategoryIds = categoryDao.categoryIds().toHashSet()
    val knownTagIds = tagDao.tagIds().toHashSet()
    val missingCategoryIds = postDtos.asSequence().flatMap { it.categories.asSequence() }.toHashSet() - knownCategoryIds
    val missingTagIds = postDtos.asSequence().flatMap { it.tags.asSequence() }.toHashSet() - knownTagIds
    return MissingMeta(missingCategoryIds, missingTagIds)
}

fun Db.importCategoryDtos(categoryDtos: List<CategoryDto>) {
    categoryDao.insertCategories(
            categoryDtos.map {
                Category(
                        id = it.id,
                        slug = it.slug,
                        count = it.count,
                        description = it.description,
                        name = it.name
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
                        name = it.name
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
                        title = postDto.title.rendered
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