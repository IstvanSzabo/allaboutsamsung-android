package de.maxisma.allaboutsamsung

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.notification.updatePushSubscription
import de.maxisma.allaboutsamsung.post.PostFragment
import de.maxisma.allaboutsamsung.posts.PostsFragment

private const val EXTRA_POST_ID = "post_id" // TODO Handle this

fun mainActivityIntent(context: Context, postId: PostId?): Intent {
    return Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_POST_ID, postId)
    }
}

class MainActivity : AppCompatActivity(), PostsFragment.InteractionListener {

    /*
     * Features TODO:
     * - Small and large widget
     * - Gallery
     * - Configuration (dark theme, push, analytics, ...)
     * - Analytics (article openings etc.)
     * - Push
     * - Featured
     * - Post overview (by category, search query, pull-to-refresh, refresh onResume after a while)
     * - YouTube channel (notify about new videos in app)
     * - Offline posts?
     * - Sharing
     * - Colors
     * - Transitions
     * - Ads
     * - Resolve warnings, code inspections
     * - Documentation
     * - Test on older versions
     * - Automatic settings migration and/or setup wizard
     * - Rotation
     * - Send app version header
     * - Highlight breaking (font color / shaking exclamation mark / ...)
     *
     * - Keep posts, tags, categories in an observable DB, observe that in ViewModels
     */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.legal_notice -> CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
                .launchUrl(this, Uri.parse(BuildConfig.LEGAL_NOTICE_URL))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updatePushSubscription(app.appComponent.db)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, PostsFragment(), "posts")
                .commit()
        }
    }

    override fun displayPost(postId: PostId) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, PostFragment(postId), "post: $postId")
            .addToBackStack("post: $postId")
            .commit()
    }
}
