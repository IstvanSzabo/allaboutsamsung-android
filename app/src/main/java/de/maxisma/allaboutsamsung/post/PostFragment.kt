package de.maxisma.allaboutsamsung.post

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
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
import de.maxisma.allaboutsamsung.utils.ExtendedWebChromeClient
import de.maxisma.allaboutsamsung.utils.observe
import kotlinx.android.synthetic.main.fragment_post.*
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
    lateinit var postHtmlGenerator: PostHtmlGenerator

    private val postId get() = arguments!!.getLong(ARG_POST_ID)

    private val commentsUrl get() = BuildConfig.COMMENTS_URL_TEMPLATE.replace("[POST_ID]", postId.toString())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent.inject(this)
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
            webViewClient = PostWebViewClient()
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

        // TODO Don't just assume the post has already been downloaded
        db.postMetaDao.postWithAuthorName(postId).observe(this) { postWithAuthorName ->
            val (post, authorName) = postWithAuthorName!!

            // TODO Test with large articles
            // TODO Catch image loading, open in gallery
            // TODO Allow video fullscreen?
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

    private fun onBottomNavigation(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.postContent -> postViewPager.currentItem = 0
            R.id.postComments -> postViewPager.currentItem = 1
            else -> throw IllegalArgumentException("Unknown menuItem with id ${menuItem.itemId}!")
        }
        return true
    }
}

private class PostWebViewClient : GlideCachingWebViewClient() {
    private fun shouldOverrideUrlLoadingInternal(view: WebView, url: String): Boolean {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(view.context, R.color.colorPrimary))
            .build()
            .launchUrl(view.context, Uri.parse(url))
        return true
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