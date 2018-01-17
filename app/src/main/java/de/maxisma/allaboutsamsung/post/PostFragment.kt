package de.maxisma.allaboutsamsung.post

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import de.maxisma.allaboutsamsung.BaseFragment
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PostId
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
    private val postId get() = arguments!!.getLong(ARG_POST_ID)
    @Inject
    lateinit var db: Db


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

        postContentWebView.settings.apply {
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptEnabled = true
        }
        postContentWebView.webViewClient = GlideCachingWebViewClient()
        postContentWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                postContentProgressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                postContentProgressBar.progress = newProgress
            }
        }

        postBottomNavigation.setOnNavigationItemSelectedListener(::onBottomNavigation)

        db.postMetaDao.postWithAuthorName(postId).observe(this) { postWithAuthorName ->
            val (post, authorName) = postWithAuthorName!!

            // TODO Test with large articles
            // TODO Catch image loading, open in gallery
            // TODO Open all links in Chrome custom tab, youtube in youtube app
            // TODO Allow video fullscreen?
            postContentWebView.loadDataWithBaseURL(BuildConfig.WEBVIEW_BASE_URL, post.toHtml(authorName), "text/html", Charsets.UTF_8.name(), null)
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