package de.maxisma.allaboutsamsung.post

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.persistence.room.Room
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PostId
import kotlinx.android.synthetic.main.fragment_post.*

private const val ARG_POST_ID = "post_id"

@Suppress("FunctionName", "DEPRECATION")
fun PostFragment(postId: PostId) = PostFragment().apply {
    arguments = Bundle().apply {
        putLong(ARG_POST_ID, postId)
    }
}

class PostFragment @Deprecated("Use factory function.") constructor() : Fragment() {
    val postId get() = arguments!!.getLong(ARG_POST_ID)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postWebView.settings.apply {
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptEnabled = true
        }
        postWebView.webViewClient = GlideCachingWebViewClient()

        // TODO Inject this
        val db = Room.databaseBuilder(context!!, Db::class.java, "db").build()
        db.postMetaDao.postWithAuthorName(postId).observe(this, Observer { postWithAuthorName ->
            val (post, authorName) = postWithAuthorName!!

            // TODO Test with large articles
            // TODO Catch image loading, open in gallery
            // TODO Open all links in Chrome custom tab, youtube in youtube app
            // TODO Allow video fullscreen?
            postWebView.loadDataWithBaseURL(BuildConfig.WEBVIEW_BASE_URL, post.toHtml(authorName), "text/html", Charsets.UTF_8.name(), null)
        })
    }
}