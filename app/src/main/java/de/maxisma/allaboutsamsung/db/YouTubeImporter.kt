package de.maxisma.allaboutsamsung.db

import de.maxisma.allaboutsamsung.utils.IOPool
import de.maxisma.allaboutsamsung.youtube.PlaylistResultDto
import kotlinx.coroutines.experimental.launch
import java.util.Date

private const val EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L

fun Db.importPlaylistResult(playlistResultDto: PlaylistResultDto) = launch (IOPool){
    val expiryDate = Date(System.currentTimeMillis() + EXPIRY_MS)
    val videos = playlistResultDto.playlist.map { Video(it.videoId, it.title, it.thumbnailUrl, Date(it.utcEpochMs), expiryDate) }
    val playlistItems = videos.map { PlaylistItem(playlistResultDto.playlistId, it.id) }

    videoDao.insertVideos(videos)
    videoDao.insertPlaylistItems(playlistItems)
}