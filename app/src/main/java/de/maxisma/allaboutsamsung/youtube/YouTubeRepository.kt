package de.maxisma.allaboutsamsung.youtube

import androidx.lifecycle.LiveData
import com.google.api.services.youtube.YouTube
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PlaylistId
import de.maxisma.allaboutsamsung.db.SeenVideo
import de.maxisma.allaboutsamsung.db.Video
import de.maxisma.allaboutsamsung.db.VideoId
import de.maxisma.allaboutsamsung.db.importPlaylistResult
import de.maxisma.allaboutsamsung.utils.DbWriteDispatcher
import de.maxisma.allaboutsamsung.utils.IOPool
import de.maxisma.allaboutsamsung.utils.retry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

typealias UnseenVideos = List<VideoId>

/**
 * Handles downloading and DB caching
 */
class YouTubeRepository(
    private val db: Db,
    private val youTube: YouTube,
    private val apiKey: String,
    private val playlistId: PlaylistId,
    private val onError: (IOException) -> Unit
) {

    val videos: LiveData<List<Video>> = db.videoDao.videosInPlaylist(playlistId)
    private val mutex = Mutex()

    /**
     * pageTokens where pageTokens[0] is the token for page 1, since page 0 does not have one.
     */
    private val pageTokens = mutableListOf<String>()

    init {
        GlobalScope.launch(DbWriteDispatcher) {
            mutex.withLock {
                db.videoDao.deleteExpired()
            }
        }
    }

    /**
     * Set that the user has seen these videos and does not need to be notified about them anymore
     */
    fun markAsSeen(unseenVideos: UnseenVideos) = GlobalScope.launch(DbWriteDispatcher) {
        mutex.withLock {
            db.videoDao.upsertSeenVideos(unseenVideos.map { SeenVideo(it) })
        }
    }

    /**
     * Refresh the DB cache from YouTube.
     *
     * @return Videos that have been newly added to the DB and have thus not been seen by the user yet
     */
    suspend fun requestNewerVideos(): UnseenVideos = withContext(IOPool) {
        mutex.withLock {
            try {
                retry(IOException::class) {
                    val playlistResultDto = youTube.downloadPlaylist(apiKey, playlistId)
                    pageTokens.clear()
                    if (playlistResultDto.nextPageToken != null) {
                        pageTokens += playlistResultDto.nextPageToken
                    }
                    withContext(DbWriteDispatcher) {
                        db.importPlaylistResult(playlistResultDto).join()
                    }

                    val seenVideos = db.videoDao.seenVideos().map { it.id }.toHashSet()
                    playlistResultDto.playlist.map { it.videoId } - seenVideos
                }
            } catch (e: IOException) {
                e.printStackTrace()
                onError(e)
                emptyList<VideoId>()
            }
        }
    }

    /**
     * Download and import videos older than the currently oldest one stored in the DB.
     */
    suspend fun requestOlderVideos() = withContext(IOPool) {
        mutex.withLock {
            try {
                retry(IOException::class) {
                    val oldestDateDb = db.videoDao.oldestDateInPlaylist(playlistId).time
                    val allResults = mutableListOf<PlaylistResultDto>()
                    while (true) {
                        val results = youTube.downloadPlaylist(apiKey, playlistId, pageTokens.lastOrNull())
                        allResults += results
                        if (results.nextPageToken != null) {
                            pageTokens += results.nextPageToken
                        } else {
                            break
                        }
                        if (results.playlist.any { it.utcEpochMs < oldestDateDb }) {
                            // We finally fetched a video older than the currently oldest one
                            break
                        }
                    }
                    launch(DbWriteDispatcher) {
                        allResults.forEach { db.importPlaylistResult(it) }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                onError(e)
            }
        }
    }

}