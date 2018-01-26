package de.maxisma.allaboutsamsung

import android.content.Context
import android.content.Intent
import android.os.Bundle
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.post.newPostActivityIntent
import de.maxisma.allaboutsamsung.posts.PostsFragment
import de.maxisma.allaboutsamsung.settings.updatePushSubscriptionsAccordingly

fun newMainActivityIntent(context: Context) = Intent(context, MainActivity::class.java)

class MainActivity : BaseActivity(), PostsFragment.InteractionListener {

    /*
     * Features TODO:
     * - Small and large widget
     * - Remaining configuration options, honour theme + analytics
     * - Analytics (article openings etc.)
     * - Featured
     * - Post overview (by category, search query, refresh onResume after a while)
     * - YouTube channel (notify about new videos in app)
     * - Test offline access
     * - Sharing
     * - Colors
     * - Ads
     * - Resolve warnings, code inspections
     * - Documentation
     * - Test on older versions
     * - Automatic settings migration and/or setup wizard
     * - Rotation
     * - Send app version header
     * - Highlight breaking (font color / shaking exclamation mark / ...)
     * - Check for memory leaks
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        app.appComponent.preferenceHolder.updatePushSubscriptionsAccordingly(app.appComponent.db)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, PostsFragment(), "posts")
                .commit()
        }
    }

    override fun displayPost(postId: PostId) {
        startActivity(newPostActivityIntent(this, postId))
    }
}
