package de.maxisma.allaboutsamsung.youtube

import com.google.api.services.youtube.YouTube
import de.maxisma.allaboutsamsung.utils.IOPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.io.IOException

data class YouTubeVideoDto(val title: String, val thumbnailUrl: String, val videoId: String, val utcEpochMs: Long)
data class PlaylistResultDto(val playlist: List<YouTubeVideoDto>, val nextPageToken: String?, val playlistId: String)

const val YOUTUBE_MAX_ITEMS_PER_REQUEST = 20

/**
 * Download a subset of items belonging to this playlist.
 *
 * @param pageToken If null, load the first page. Otherwise load the page represented by this token.
 */
fun YouTube.downloadPlaylist(apiKey: String, playlistId: String, pageToken: String? = null): Deferred<PlaylistResultDto> = async(IOPool) {
    val playlist = playlistItems().list("id,snippet").apply {
        key = apiKey
        maxResults = YOUTUBE_MAX_ITEMS_PER_REQUEST.toLong()
        this.playlistId = playlistId
        if (pageToken != null) {
            this.pageToken = pageToken
        }
    }
    val response = playlist.execute()
    if (response.kind == "youtube#playlistItemListResponse") {
        val videos = response.items.map {
            YouTubeVideoDto(it.snippet.title, it.snippet.thumbnails.high.url, it.snippet.resourceId.videoId, it.snippet.publishedAt.value)
        }
        return@async PlaylistResultDto(videos, response.nextPageToken, playlistId)
    } else {
        throw IOException("Error while loading from API")
    }
}