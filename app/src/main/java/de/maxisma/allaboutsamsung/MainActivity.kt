package de.maxisma.allaboutsamsung

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.post.PostFragment
import de.maxisma.allaboutsamsung.posts.PostsFragment

class MainActivity : AppCompatActivity(), PostsFragment.InteractionListener {

    /*
     * Features TODO:
     * - Small and large widget
     * - Legal notice?
     * - Liveblog?
     * - Gallery
     * - Configuration (dark theme, push, analytics, ...)
     * - Analytics (article openings etc.)
     * - Push
     * - Featured
     * - Post overview (by category, search query, pull-to-refresh, refresh onResume after a while)
     * - Post details + comments
     * - YouTube channel (notify about new videos in app)
     * - Offline posts?
     * - Sharing
     * - Glide preload
     * - Colors
     * - Transitions
     * - Ads
     * - Resolve warnings, code inspections
     * - Documentation
     *
     * - Keep posts, tags, categories in an observable DB, observe that in ViewModels
     *
     * - DEPENDENCY INJECTION
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, PostsFragment(), "posts")
                .addToBackStack("posts")
                .commit()
    }

    override fun displayPost(postId: PostId) {
        supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, PostFragment(postId), "post: $postId")
                .addToBackStack("post: $postId")
                .commit()
    }
}
