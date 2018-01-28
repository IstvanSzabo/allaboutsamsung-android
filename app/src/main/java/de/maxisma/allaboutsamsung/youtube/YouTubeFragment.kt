package de.maxisma.allaboutsamsung.youtube

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.support.annotation.MainThread
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener
import com.google.api.services.youtube.YouTube
import de.maxisma.allaboutsamsung.BaseFragment
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Video
import de.maxisma.allaboutsamsung.posts.SpacingItemDecoration
import de.maxisma.allaboutsamsung.utils.dpToPx
import de.maxisma.allaboutsamsung.utils.observe
import kotlinx.android.synthetic.main.fragment_youtube.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

/**
 * Avoid that on first launch the user is notified about new
 * videos even though he has installed it for the first time.
 */
private const val NOTIFY_NEW_VIDEOS_IF_LESS_THAN = 6

class YouTubeFragment : BaseFragment<YouTubeFragment.InteractionListener>() {

    interface InteractionListener {
        fun notifyUnseenVideos(howMany: Int)
    }

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var youTube: YouTube

    private lateinit var repo: YouTubeRepository

    private var currentLoadingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent.inject(this)

        repo = YouTubeRepository(db, youTube, BuildConfig.YOUTUBE_API_KEY, BuildConfig.YOUTUBE_PLAYLIST_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_youtube, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videosSwipeRefresh.setOnRefreshListener { requestNewerVideos() }

        val adapter = VideoAdapter { it.fireIntent() }
        val lm = LinearLayoutManager(context!!)
        videoList.adapter = adapter
        videoList.layoutManager = lm
        videoList.addItemDecoration(SpacingItemDecoration(horizontalSpacing = 8.dpToPx(), verticalSpacing = 8.dpToPx()))

        val infiniteScrollListener = object : InfiniteScrollListener(YOUTUBE_MAX_ITEMS_PER_REQUEST, lm) {
            override fun onScrolledToEnd(firstVisibleItemPosition: Int) {
                requestOlderVideos()
            }
        }
        videoList.addOnScrollListener(infiniteScrollListener)

        repo.videos.observe(this) {
            adapter.videos = it ?: return@observe
            adapter.notifyDataSetChanged()
        }

        requestNewerVideos()
    }

    @MainThread
    private fun debounceLoad(f: suspend () -> Unit) {
        require(Looper.getMainLooper() == Looper.myLooper()) { "Must be run on UI thread!" }

        if (currentLoadingJob?.isActive != true) {
            currentLoadingJob = launch(UI) {
                videosSwipeRefresh.isRefreshing = true
                f()
                videosSwipeRefresh.isRefreshing = false

                // Debounce UI interaction
                delay(500)
            }
        }
    }

    @MainThread
    private fun requestNewerVideos() {
        debounceLoad {
            val unseenVideos = repo.requestNewerVideos().await()
            repo.markAsSeen(unseenVideos).join()

            if (unseenVideos.size in 1 until NOTIFY_NEW_VIDEOS_IF_LESS_THAN) {
                listener.notifyUnseenVideos(unseenVideos.size)
            }
        }
    }

    @MainThread
    private fun requestOlderVideos() {
        debounceLoad { repo.requestOlderVideos().join() }
    }

    private fun Video.fireIntent() {
        val url = "https://www.youtube.com/watch?v=$id"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

}
