package de.maxisma.allaboutsamsung.post

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.viewpager.widget.ViewPager
import de.maxisma.allaboutsamsung.BaseFragment
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.gallery.Photo
import de.maxisma.allaboutsamsung.gallery.extractPhotos
import de.maxisma.allaboutsamsung.gallery.newGalleryActivityIntent
import de.maxisma.allaboutsamsung.post.html.PostHtmlGenerator
import de.maxisma.allaboutsamsung.post.html.generateAnalyticsJs
import de.maxisma.allaboutsamsung.post.html.obtainHtmlThemes
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.rest.loadCommentsFor
import de.maxisma.allaboutsamsung.settings.PreferenceHolder
import de.maxisma.allaboutsamsung.utils.ExtendedWebChromeClient
import de.maxisma.allaboutsamsung.utils.observe
import de.maxisma.allaboutsamsung.utils.observeUntilFalse
import kotlinx.android.synthetic.main.fragment_post.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import javax.inject.Inject

private const val ARG_POST_ID = "post_id"

@Suppress("FunctionName", "DEPRECATION")
fun PostFragment(postId: PostId) = PostFragment().apply {
    arguments = Bundle().apply {
        putLong(ARG_POST_ID, postId)
    }
}

private const val POST_DETAIL_EXPIRY_MS = 2 * 60 * 1000L

class PostFragment @Deprecated("Use factory function.") constructor() : BaseFragment<PostFragment.Listener>() {

    interface Listener {
        val fullScreenViewContainer: ViewGroup
    }

    @Inject
    lateinit var db: Db

    @Inject
    lateinit var wordpressApi: WordpressApi

    @Inject
    lateinit var postHtmlGenerator: PostHtmlGenerator

    @Inject
    lateinit var preferenceHolder: PreferenceHolder

    @Inject
    lateinit var httpClient: OkHttpClient

    @Inject
    lateinit var keyValueStore: KeyValueStore

    private val postId: PostId by lazy { arguments!!.getLong(ARG_POST_ID) }

    private val theme
        get() = run {
            val themes = context!!.obtainHtmlThemes()
            if (preferenceHolder.useDarkTheme) themes.darkTheme else themes.lightTheme
        }

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
        // Load default HTML to set background color
        for (webView in arrayOf(postContentWebView, postCommentsWebView)) {
            webView.loadData(postHtmlGenerator.generateEmptyHtml(theme), "text/html", Charsets.UTF_8.name())
        }

        super.onViewCreated(view, savedInstanceState)

        postContentWebView.apply {
            settings.apply {
                loadWithOverviewMode = true
                useWideViewPort = true
                javaScriptEnabled = !preferenceHolder.gdprMode
            }
            webChromeClient = ExtendedWebChromeClient(
                postContentProgressBar,
                supportWindowCreation = false,
                fragment = this@PostFragment,
                customViewContainer = listener.fullScreenViewContainer,
                customViewActiveListener = { active ->
                    (activity as? AppCompatActivity)?.supportActionBar?.let { if (active) it.hide() else it.show() }
                }
            )
        }
        postCommentsWebView.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }
            settings.apply {
                javaScriptEnabled = !preferenceHolder.gdprMode
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
            }
            webChromeClient = ExtendedWebChromeClient(postCommentsProgressBar, supportWindowCreation = true, fragment = this@PostFragment)
        }

        postBottomNavigation.setOnNavigationItemSelectedListener(::onBottomNavigation)
        postViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                postBottomNavigation.selectedItemId = postBottomNavigation.menu.getItem(position).itemId
            }
        })

        val query = Query.Filter(onlyIds = listOf(postId))
        val executor = query.newExecutor(wordpressApi, db, keyValueStore, coroutineScope = this, onError = ::displaySupportedError)

        db.postMetaDao.postWithAuthorName(postId).observe(this) { postWithAuthorName ->
            val (post, authorName) = postWithAuthorName ?: return@observe run { executor.requestNewerPosts() }

            if (post.dbItemCreatedDateUtc.time + POST_DETAIL_EXPIRY_MS < System.currentTimeMillis()) {
                executor.refresh(postId)
            }

            val analyticsJs = if (preferenceHolder.allowAnalytics) post.generateAnalyticsJs() else ""

            postContentWebView.webViewClient = PostWebViewClient(post.extractPhotos(), preferenceHolder.allowedHosts)
            postContentWebView.loadDataWithBaseURL(
                BuildConfig.WEBVIEW_BASE_URL,
                postHtmlGenerator.generateHtml(post, authorName, theme, analyticsJs, keyValueStore.adHtml ?: ""),
                "text/html",
                Charsets.UTF_8.name(),
                null
            )

            if (!preferenceHolder.gdprMode) {
                loadCommentsFor(postCommentsWebView, postId, httpClient, theme, onError = ::displaySupportedError)
            }
        }
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

    private val PreferenceHolder.allowedHosts: Set<String>?
        get() = if (gdprMode) {
            sequenceOf(BuildConfig.REST_BASE_URL, BuildConfig.APP_API_BASE_URL).mapNotNull { HttpUrl.parse(it)?.host() }.toSet()
        } else {
            null
        }
}

private class PostWebViewClient(private val photos: List<Photo>, allowedHosts: Set<String>?) : GlideCachingWebViewClient(allowedHosts) {
    private fun shouldOverrideUrlLoadingInternal(view: WebView, url: String): Boolean {
        val uri = url.toUri()
        val context = view.context
        val postUrlIntent = Intent(Intent.ACTION_VIEW, uri)
        val intentActivities = context.packageManager.queryIntentActivities(postUrlIntent, 0)

        when {
            intentActivities.any {
                it.activityInfo.packageName == context.packageName &&
                        it.activityInfo.name.endsWith(PostActivity::class.java.simpleName)
            } -> {
                context.startActivity(postUrlIntent)
            }
            url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") -> {
                val selected = photos.firstOrNull { url == it.fullImageUrl || url == it.smallImageUrl }
                if (selected != null) {
                    val intent = newGalleryActivityIntent(context, photos, selected)
                    context.startActivity(intent)
                } else {
                    openCustomTab(context, url)
                }
            }
            "youtube." in HttpUrl.parse(url)?.host() ?: "" -> context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            uri.scheme?.startsWith("file") == true -> Unit // Ignore
            else -> try {
                openCustomTab(context, url)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(view.context, R.string.broken_link, Toast.LENGTH_SHORT).show()
            }
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