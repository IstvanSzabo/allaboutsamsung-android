package de.maxisma.allaboutsamsung.gallery

import de.maxisma.allaboutsamsung.db.Post
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag

private fun Element.findFullImgUrl(): String? {
    require(tag() == Tag.valueOf("img")) { "Needs to be an img element!" }
    val parentLink = parents().firstOrNull { it.tag() == Tag.valueOf("a") }
    return parentLink?.attr("href")
}

fun Post.extractPhotos(): List<Photo> {
    val doc = Jsoup.parse(content)
    val images = doc.getElementsByTag("img").not(".wp-smiley")
    return images.map { img ->
        val small = img.attr("src")
        val full = img.findFullImgUrl()
        Photo(small, full ?: small)
    }
}