package de.maxisma.allaboutsamsung.gallery

import de.maxisma.allaboutsamsung.db.Post
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag

/**
 * Travel up the DOM and find an enclosing <a> tag.
 */
fun Element.findFullImgUrl(): String? {
    require(tag() == Tag.valueOf("img")) { "Needs to be an img element!" }

    val srcSetUrl = srcSet()?.maxBy { (_, width) -> width }?.first
    if (srcSetUrl != null) {
        return srcSetUrl
    }

    val parentLink = parents().firstOrNull { it.tag() == Tag.valueOf("a") }
    return parentLink?.attr("href")
}

private fun Element.findSmallImgUrl(): String {
    require(tag() == Tag.valueOf("img")) { "Needs to be an img element!" }

    return srcSet()?.minBy { (_, width) -> width }?.first ?: attr("src")
}

private fun Element.findOtherImgUrls(): List<String> {
    require(tag() == Tag.valueOf("img")) { "Needs to be an img element!" }

    val parent = parent()
    return if (parent.tag() == Tag.valueOf("a") && parent.hasAttr("href")) {
        listOf(parent.attr("href"))
    } else {
        emptyList()
    }
}

private fun Element.srcSet() = attr("srcset")
    ?.split(',')
    ?.asSequence()
    ?.map { srcSpec -> srcSpec.trim().split(' ') }
    ?.filter { it.size == 2 && it[1].endsWith('w') }
    ?.mapNotNull { (src, widthStr) -> widthStr.dropLast(1).toIntOrNull()?.let { width -> src to width } }

/**
 * Find all photos in the article
 */
fun Post.extractPhotos(): List<Photo> {
    val doc = Jsoup.parse(content)
    val images = doc.getElementsByTag("img").not(".wp-smiley")
    return images
        .asSequence()
        .filter { it.classNames().any { className -> className.startsWith("wp-image") || className.startsWith("attachment") } }
        .map { img ->
            val small = img.findSmallImgUrl()
            val full = img.findFullImgUrl()
            val others = img.findOtherImgUrls()
            Photo(small, full ?: small, others)
        }
        .toList()
}