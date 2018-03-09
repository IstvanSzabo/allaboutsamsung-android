package de.maxisma.allaboutsamsung.gallery

import de.maxisma.allaboutsamsung.db.Post
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag

/**
 * Travel up the DOM and find an enclosing <a> tag.
 */
private fun Element.findFullImgUrl(): String? {
    require(tag() == Tag.valueOf("img")) { "Needs to be an img element!" }
    val parentLink = parents().firstOrNull { it.tag() == Tag.valueOf("a") }
    return parentLink?.attr("href")
}

/**
 * Find all photos in the article
 */
fun Post.extractPhotos(): List<Photo> {
    val doc = Jsoup.parse(content)
    val images = doc.getElementsByTag("img").not(".wp-smiley")
    return images
        .filter { it.classNames().any { it.startsWith("wp-image") || it.startsWith("attachment") } }
        .map { img ->
            val small = img.attr("src")
            val full = img.findFullImgUrl()
            Photo(small, full ?: small)
        }
}