package de.maxisma.allaboutsamsung.post

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.ShareCompat
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import de.maxisma.allaboutsamsung.BaseFragment
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.gallery.Photo
import de.maxisma.allaboutsamsung.gallery.extractPhotos
import de.maxisma.allaboutsamsung.gallery.newGalleryActivityIntent
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.utils.ExtendedWebChromeClient
import de.maxisma.allaboutsamsung.utils.observe
import de.maxisma.allaboutsamsung.utils.observeUntilFalse
import kotlinx.android.synthetic.main.fragment_post.*
import okhttp3.HttpUrl
import javax.inject.Inject

private const val ARG_POST_ID = "post_id"

@Suppress("FunctionName", "DEPRECATION")
fun PostFragment(postId: PostId) = PostFragment().apply {
    arguments = Bundle().apply {
        putLong(ARG_POST_ID, postId)
    }
}

class PostFragment @Deprecated("Use factory function.") constructor() : BaseFragment<Nothing>() {
    @Inject
    lateinit var db: Db

    @Inject
    lateinit var wordpressApi: WordpressApi

    @Inject
    lateinit var postHtmlGenerator: PostHtmlGenerator

    private val postId get() = arguments!!.getLong(ARG_POST_ID)

    private val commentsUrl get() = BuildConfig.COMMENTS_URL_TEMPLATE.replace("[POST_ID]", postId.toString())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        app.appComponent.inject(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_post, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.share -> {
            sharePost()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postContentWebView.apply {
            settings.apply {
                loadWithOverviewMode = true
                useWideViewPort = true
                javaScriptEnabled = true
            }
            webChromeClient = ExtendedWebChromeClient(postContentProgressBar)
        }
        postCommentsWebView.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }
            settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
            }
            webChromeClient = ExtendedWebChromeClient(postCommentsProgressBar, supportWindowCreation = true)
        }

        postBottomNavigation.setOnNavigationItemSelectedListener(::onBottomNavigation)


        db.postMetaDao.postWithAuthorName(postId).observe(this) { postWithAuthorName ->
            val (post, authorName) = postWithAuthorName ?: return@observe downloadPost(postId)

            postContentWebView.webViewClient = PostWebViewClient(post.extractPhotos())
            postContentWebView.loadDataWithBaseURL(
                BuildConfig.WEBVIEW_BASE_URL,
                postHtmlGenerator.generateHtml(post, authorName),
                "text/html",
                Charsets.UTF_8.name(),
                null
            )
            postCommentsWebView.loadUrl(commentsUrl)
        }
    }

    private fun downloadPost(postId: PostId) {
        val query = Query.Filter(onlyIds = listOf(postId))
        val executor = query.newExecutor(wordpressApi, db, ::displaySupportedError)
        executor.requestNewerPosts()
    }

    private fun sharePost() {
        db.postDao.post(postId).observeUntilFalse(this) { post ->
            post ?: return@observeUntilFalse true

            ShareCompat.IntentBuilder.from(activity)
                .setType("text/plain")
                .setText("${post.title} ${post.link}")
                .setChooserTitle(R.string.share_post)
                .startChooser()

            false
        }
    }

    private fun onBottomNavigation(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.postContent -> postViewPager.currentItem = 0
            R.id.postComments -> postViewPager.currentItem = 1
            else -> throw IllegalArgumentException("Unknown menuItem with id ${menuItem.itemId}!")
        }
        return true
    }
}

private class PostWebViewClient(private val photos: List<Photo>) : GlideCachingWebViewClient() {
    private fun shouldOverrideUrlLoadingInternal(view: WebView, url: String): Boolean {
        when {
            url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") -> {
                val selected = photos.firstOrNull { url == it.fullImageUrl || url == it.smallImageUrl }
                if (selected != null) {
                    val intent = newGalleryActivityIntent(view.context, photos, selected)
                    view.context.startActivity(intent)
                } else {
                    openCustomTab(view.context, url)
                }
            }
            "youtube." in HttpUrl.parse(url)?.host() ?: "" -> view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else -> openCustomTab(view.context, url)
        }
        return true
    }

    private fun openCustomTab(context: Context, url: String) {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .build()
            .launchUrl(context, Uri.parse(url))
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return shouldOverrideUrlLoadingInternal(view, url) || super.shouldOverrideUrlLoading(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return request.method == "GET" && shouldOverrideUrlLoadingInternal(view, request.url.toString()) || super.shouldOverrideUrlLoading(view, request)
    }
}