package de.maxisma.allaboutsamsung.youtube

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener
import com.google.api.services.youtube.YouTube
import de.maxisma.allaboutsamsung.BaseFragment
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Video
import de.maxisma.allaboutsamsung.posts.SpacingItemDecoration
import de.maxisma.allaboutsamsung.settings.PreferenceHolder
import de.maxisma.allaboutsamsung.utils.dpToPx
import de.maxisma.allaboutsamsung.utils.observe
import de.maxisma.allaboutsamsung.utils.toStyledTitle
import kotlinx.android.synthetic.main.fragment_youtube.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.math.min

/**
 * Avoid that on first launch the user is notified about new
 * videos even though he has installed it for the first time.
 */
private const val NOTIFY_NEW_VIDEOS_IF_LESS_THAN = 6

private const val STATE_LIST_POSITION = "list_position"

class YouTubeFragment : BaseFragment<YouTubeFragment.Listener>() {

    interface Listener {
        fun notifyUnseenVideos(howMany: Int)
    }

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var youTube: YouTube

    @Inject
    lateinit var preferenceHolder: PreferenceHolder

    private lateinit var repo: YouTubeRepository

    private var currentLoadingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent.inject(this)

        repo = YouTubeRepository(db, youTube, BuildConfig.YOUTUBE_API_KEY, BuildConfig.YOUTUBE_PLAYLIST_ID, ::displaySupportedError)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_youtube, container, false)
    }

    private var layoutManager: LinearLayoutManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (preferenceHolder.gdprMode) return

        videosSwipeRefresh.setOnRefreshListener { requestNewerVideos() }

        val adapter = VideoAdapter { it.fireIntent() }
        layoutManager = LinearLayoutManager(context!!)
        videoList.adapter = adapter
        videoList.layoutManager = layoutManager
        videoList.addItemDecoration(SpacingItemDecoration(horizontalSpacing = 8.dpToPx(), verticalSpacing = 8.dpToPx()))

        val infiniteScrollListener = object : InfiniteScrollListener(YOUTUBE_MAX_ITEMS_PER_REQUEST, layoutManager) {
            override fun onScrolledToEnd(firstVisibleItemPosition: Int) {
                requestOlderVideos()
            }
        }
        videoList.addOnScrollListener(infiniteScrollListener)

        val lastListPosition = if (savedInstanceState?.containsKey(STATE_LIST_POSITION) == true) {
            savedInstanceState.getInt(STATE_LIST_POSITION)
        } else {
            null
        }

        repo.videos.observe(this) { videos ->
            adapter.videos = videos?.map { VideoViewModel(it, it.title.toStyledTitle(context!!)) } ?: return@observe
            adapter.notifyDataSetChanged()
        }

        uiLaunch {
            if (lastListPosition != null) {
                videoList.setOnTouchListener { _, _ -> true }
            }

            requestNewerVideos().join()

            if (lastListPosition != null) {
                var lastCount = -1
                val videoListAdapter = videoList.adapter ?: error("No adapter set")
                while (videoListAdapter.itemCount <= lastListPosition && lastCount != videoListAdapter.itemCount) {
                    // Abort loop if item count doesn't change anymore
                    lastCount = videoListAdapter.itemCount

                    requestOlderVideos().join()
                }
                videoList.scrollToPosition(min(lastListPosition, videoListAdapter.itemCount))
                videoList.setOnTouchListener(null)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_LIST_POSITION, layoutManager?.findFirstVisibleItemPosition() ?: 0)
    }

    /**
     * Run [f] only if no other operation is currently running on [currentLoadingJob].
     */
    private fun debounceLoad(f: suspend () -> Unit) = uiLaunch {
        if (currentLoadingJob?.isActive != true) {
            currentLoadingJob = uiLaunch {
                videosSwipeRefresh.isRefreshing = true
                f()
                videosSwipeRefresh.isRefreshing = false

                // Debounce UI interaction
                delay(500)
            }
            currentLoadingJob?.join()
        }
    }

    /**
     * Refresh the video list from YouTube. If there are new videos,
     * notify the user about it.
     */
    private fun requestNewerVideos() = debounceLoad {
        val unseenVideos = repo.requestNewerVideos()
        repo.markAsSeen(unseenVideos).join()

        if (unseenVideos.size in 1 until NOTIFY_NEW_VIDEOS_IF_LESS_THAN) {
            listener.notifyUnseenVideos(unseenVideos.size)
        }
    }

    /**
     * Request older pages
     */
    private fun requestOlderVideos() = debounceLoad { repo.requestOlderVideos() }

    /**
     * Start the YouTube app for the [Video]
     */
    private fun Video.fireIntent() {
        val url = "https://www.youtube.com/watch?v=$id"
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

}
