package de.maxisma.allaboutsamsung.posts

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener
import de.maxisma.allaboutsamsung.BaseFragment
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.categories.categoryActivityResult
import de.maxisma.allaboutsamsung.categories.newCategoryActivityIntent
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.QueryExecutor
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.utils.dpToPx
import de.maxisma.allaboutsamsung.utils.observe
import kotlinx.android.synthetic.main.fragment_posts.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

private const val MAX_ITEMS_PER_REQUEST_ON_SCROLL = 20
private const val REQUEST_CODE_CATEGORY = 0

class PostsFragment : BaseFragment<PostsFragment.InteractionListener>() {

    interface InteractionListener {
        fun displayPost(postId: PostId)
    }

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var wordpressApi: WordpressApi

    private var currentLoadingJob: Job? = null
    private var currentExecutor: QueryExecutor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        app.appComponent.inject(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_posts, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                requestNewerPosts()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO Make buttons highlight on click
        categoryButton.setOnClickListener {
            startActivityForResult(newCategoryActivityIntent(context!!), REQUEST_CODE_CATEGORY)
        }

        postsSwipeRefresh.setOnRefreshListener { requestNewerPosts() }

        val adapter = PostsAdapter { listener.displayPost(it.id) }
        val lm = LinearLayoutManager(context!!)
        postList.adapter = adapter
        postList.layoutManager = lm
        postList.addItemDecoration(SpacingItemDecoration(horizontalSpacing = 8.dpToPx(), verticalSpacing = 8.dpToPx()))

        val infiniteScrollListener = object : InfiniteScrollListener(MAX_ITEMS_PER_REQUEST_ON_SCROLL, lm) {

            override fun onScrolledToEnd(firstVisibleItemPosition: Int) {
                if (currentLoadingJob?.isActive != true) {
                    currentLoadingJob = launch(UI) {
                        postsSwipeRefresh.isRefreshing = true
                        currentExecutor?.requestOlderPosts()?.join()
                        postsSwipeRefresh.isRefreshing = false
                        // Debounce UI interaction
                        delay(500)
                    }
                }
            }
        }
        postList.addOnScrollListener(infiniteScrollListener)

        Query.Empty.load()
    }

    private fun Query.load() {
        currentExecutor?.data?.removeObservers(this@PostsFragment)

        val executor = newExecutor(wordpressApi, db, ::displaySupportedError)
        val adapter = postList.adapter as PostsAdapter
        executor.data.observe(this@PostsFragment) { it ->
            adapter.posts = it ?: emptyList()
            adapter.notifyDataSetChanged()
        }

        currentExecutor = executor
        requestNewerPosts()
    }

    private fun requestNewerPosts() = launch(UI) {
        postsSwipeRefresh.isRefreshing = true
        currentExecutor?.requestNewerPosts()?.join()
        postsSwipeRefresh.isRefreshing = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val id = data?.categoryActivityResult?.categoryId
        if (requestCode == REQUEST_CODE_CATEGORY && id != null) {
            Query.Filter(onlyCategories = listOf(id)).load()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}