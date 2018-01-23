package de.maxisma.allaboutsamsung.posts

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import de.maxisma.allaboutsamsung.BaseFragment
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.utils.dpToPx
import de.maxisma.allaboutsamsung.utils.observe
import kotlinx.android.synthetic.main.fragment_posts.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val MAX_ITEMS_PER_REQUEST_ON_SCROLL = 20

class PostsFragment : BaseFragment<PostsFragment.InteractionListener>() {

    interface InteractionListener {
        fun displayPost(postId: PostId)
    }

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var wordpressApi: WordpressApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_posts, container, false)
    }

    private fun onError(e: Exception) {
        val message = when(e) {
            is HttpException, is JsonDataException, is JsonEncodingException -> R.string.server_error
            is IOException, is TimeoutCancellationException -> R.string.network_error
            else -> throw e
        }
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = Query.Empty
        val executor = query.newExecutor(wordpressApi, db, ::onError)

        val adapter = PostsAdapter { listener.displayPost(it.id) }
        val lm = LinearLayoutManager(context!!)
        postList.adapter = adapter
        postList.layoutManager = lm
        postList.addItemDecoration(SpacingItemDecoration(horizontalSpacing = 8.dpToPx(), verticalSpacing = 8.dpToPx()))

        val infiniteScrollListener = object : InfiniteScrollListener(MAX_ITEMS_PER_REQUEST_ON_SCROLL, lm) {

            private var lastJob: Job? = null

            override fun onScrolledToEnd(firstVisibleItemPosition: Int) {
                if (lastJob?.isActive != true) {
                    lastJob = launch(UI) {
                        postsProgressBar.visibility = View.VISIBLE
                        executor.requestOlderPosts().join()
                        postsProgressBar.visibility = View.GONE
                        // Debounce UI interaction
                        delay(500)
                    }
                }
            }
        }
        postList.addOnScrollListener(infiniteScrollListener)

        executor.data.observe(this) { it ->
            adapter.posts = it ?: emptyList()
            adapter.notifyDataSetChanged()
        }

        launch(UI) {
            postsProgressBar.visibility = View.VISIBLE
            executor.requestNewerPosts()
            postsProgressBar.visibility = View.GONE
        }
    }
}