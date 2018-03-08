package de.maxisma.allaboutsamsung.posts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener
import de.maxisma.allaboutsamsung.BaseFragment
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.ad.updateAdHtml
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.categories.categoryActivityResult
import de.maxisma.allaboutsamsung.categories.newCategoryActivityIntent
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.QueryExecutor
import de.maxisma.allaboutsamsung.query.WORDPRESS_POSTS_PER_PAGE
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.settings.PreferenceHolder
import de.maxisma.allaboutsamsung.utils.IOPool
import de.maxisma.allaboutsamsung.utils.dpToPx
import de.maxisma.allaboutsamsung.utils.observe
import de.maxisma.allaboutsamsung.utils.trackLandingLoad
import kotlinx.android.synthetic.main.fragment_posts.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

private const val REQUEST_CODE_CATEGORY = 0

private const val RELOAD_AFTER_MS = 30 * 60 * 1000L

class PostsFragment : BaseFragment<PostsFragment.InteractionListener>() {

    interface InteractionListener {
        fun displayPost(postId: PostId)
    }

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var wordpressApi: WordpressApi

    @Inject
    lateinit var preferenceHolder: PreferenceHolder

    @Inject
    lateinit var keyValueStore: KeyValueStore

    private var currentLoadingJob: Job? = null
    private var currentExecutor: QueryExecutor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        app.appComponent.inject(this)
    }

    private lateinit var searchItem: MenuItem

    private val searchQueryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean {
            Query.Filter(string = query).load()

            (searchItem.actionView as SearchView).isIconified = true
            searchItem.collapseActionView()
            return false
        }

        override fun onQueryTextChange(newText: String?) = false

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_posts, menu)

        searchItem = menu.findItem(R.id.search)!!
        (searchItem.actionView as SearchView).setOnQueryTextListener(searchQueryListener)
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

        categoryButton.setOnClickListener {
            startActivityForResult(newCategoryActivityIntent(context!!), REQUEST_CODE_CATEGORY)
        }
        searchButton.setOnClickListener { searchItem.expandActionView() }

        postsSwipeRefresh.setOnRefreshListener { requestNewerPosts() }

        val showAd = !BuildConfig.DEBUG && getString(R.string.appmobPostListAdId).isNotEmpty()
        val adapter = PostsAdapter(showAd = showAd) { listener.displayPost(it.id) }
        val lm = LinearLayoutManager(context!!)
        postList.adapter = adapter
        postList.layoutManager = lm
        postList.addItemDecoration(SpacingItemDecoration(horizontalSpacing = 8.dpToPx(), verticalSpacing = 8.dpToPx()))

        val infiniteScrollListener = object : InfiniteScrollListener(WORDPRESS_POSTS_PER_PAGE, lm) {

            override fun onScrolledToEnd(firstVisibleItemPosition: Int) {
                if (currentLoadingJob?.isActive != true) {
                    currentLoadingJob = launch(UI) {
                        requestOlderPosts()
                        // Debounce UI interaction
                        delay(500)
                    }
                }
            }
        }
        postList.addOnScrollListener(infiniteScrollListener)

        Query.Empty.load()
        db.postDao.latestActiveBreakingPost().observe(this) { post ->
            if (post == null) {
                featured.visibility = View.GONE
            } else {
                featured.visibility = View.VISIBLE
                featured.setOnClickListener { listener.displayPost(post.id) }
                featured.text = getString(R.string.breaking_featured_template, post.title)
            }
        }
    }

    private fun Query.load() = launch(UI) {
        currentExecutor?.data?.removeObservers(this@PostsFragment)

        val executor = newExecutor(wordpressApi, db, ::displaySupportedError)
        val adapter = postList.adapter as PostsAdapter
        executor.data.observe(this@PostsFragment) {
            adapter.updateWith(it ?: emptyList(), executor)
        }

        currentExecutor = executor
        requestNewerPosts()
        activity?.title = description.await()

        keyValueStore.updateAdHtml()

        if (preferenceHolder.allowAnalytics) {
            trackLandingLoad(this@PostsFragment.context!!)
        }
    }

    private fun PostsAdapter.updateWith(posts: List<Post>, executor: QueryExecutor) = launch(UI) {
        this@updateWith.posts = posts.toPostViewModels(executor)
        notifyDataSetChanged()
    }

    private suspend fun Iterable<Post>.toPostViewModels(executor: QueryExecutor) =
        map { PostViewModel(it, isBreaking = BuildConfig.BREAKING_CATEGORY_ID in executor.categoriesForPost(it.id).await().map { it.id }) }

    private val Query.description: Deferred<String>
        get() = when (this) {
            Query.Empty -> async { getString(R.string.app_name) }
            is Query.Filter -> when {
                string != null -> async<String> { string }
                onlyCategories?.size == 1 -> async(IOPool) { db.categoryDao.categories(onlyCategories)[0].name }
                else -> async { getString(R.string.app_name) }
            }
        }

    private var lastLoadTimeMs = -1L

    private fun requestNewerPosts(): Job {
        lastLoadTimeMs = System.currentTimeMillis()
        return launch(UI) {
            postsSwipeRefresh.isRefreshing = true
            currentExecutor?.requestNewerPosts()?.join()
            postsSwipeRefresh.isRefreshing = false
        }
    }

    private fun requestOlderPosts(): Job {
        lastLoadTimeMs = System.currentTimeMillis()
        return launch(UI) {
            postsSwipeRefresh.isRefreshing = true
            currentExecutor?.requestOlderPosts()?.join()
            postsSwipeRefresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()

        if (lastLoadTimeMs + RELOAD_AFTER_MS < System.currentTimeMillis()) {
            requestNewerPosts()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CATEGORY && resultCode == Activity.RESULT_OK) {
            val id = data?.categoryActivityResult?.categoryId
            val query = if (id != null) Query.Filter(onlyCategories = listOf(id)) else Query.Empty

            if (query != currentExecutor?.query) {
                query.load()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}