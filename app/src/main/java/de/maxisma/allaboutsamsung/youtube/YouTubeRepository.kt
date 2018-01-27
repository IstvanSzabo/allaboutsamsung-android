package de.maxisma.allaboutsamsung.youtube

import android.arch.lifecycle.LiveData
import com.google.api.services.youtube.YouTube
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PlaylistId
import de.maxisma.allaboutsamsung.db.Video
import de.maxisma.allaboutsamsung.db.importPlaylistResult
import de.maxisma.allaboutsamsung.utils.IOPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class YouTubeRepository(
    private val db: Db,
    private val youTube: YouTube,
    private val apiKey: String,
    private val playlistId: PlaylistId
) {

    val videos: LiveData<List<Video>> = db.videoDao.videosInPlaylist(playlistId)
    private val mutex = Mutex()

    /**
     * pageTokens where pageTokens[0] is the token for page 1, since page 0 does not have one.
     */
    private val pageTokens = mutableListOf<String>()

    fun requestNewerVideos(): Job = launch(IOPool) {
        mutex.withLock {
            val playlistResultDto = youTube.downloadPlaylist(apiKey, playlistId).await()
            if (pageTokens.isNotEmpty()) {
                pageTokens[0] = playlistResultDto.nextPageToken
            } else {
                pageTokens += playlistResultDto.nextPageToken
            }
            db.importPlaylistResult(playlistResultDto).join()
        }
    }

    fun requestOlderVideos(): Job = launch(IOPool) {
        mutex.withLock {
            val oldestDateDb = db.videoDao.oldestDateInPlaylist(playlistId).time
            val allResults = mutableListOf<PlaylistResultDto>()
            while (true) {
                val results = youTube.downloadPlaylist(apiKey, playlistId, pageTokens.lastOrNull()).await()
                allResults += results
                pageTokens += results.nextPageToken
                if (results.playlist.any { it.utcEpochMs < oldestDateDb }) {
                    // We finally fetched a video older than the currently oldest one
                    break
                }
            }
            allResults.forEach { db.importPlaylistResult(it) }
        }
    }

}