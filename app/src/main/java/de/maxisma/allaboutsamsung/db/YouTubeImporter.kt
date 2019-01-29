package de.maxisma.allaboutsamsung.db

import de.maxisma.allaboutsamsung.utils.DbWriteDispatcher
import de.maxisma.allaboutsamsung.youtube.PlaylistResultDto
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Date

private const val EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L

/**
 * Cache the playlist and referenced videos in the DB.
 */
fun Db.importPlaylistResult(playlistResultDto: PlaylistResultDto) = GlobalScope.launch(DbWriteDispatcher) {
    val expiryDate = Date(System.currentTimeMillis() + EXPIRY_MS)
    val videos = playlistResultDto.playlist.map { Video(it.videoId, it.title, it.thumbnailUrl, Date(it.utcEpochMs), expiryDate) }
    val playlistItems = videos.map { PlaylistItem(playlistResultDto.playlistId, it.id) }

    videoDao.upsertVideos(videos)
    videoDao.upsertPlaylistItems(playlistItems)
}