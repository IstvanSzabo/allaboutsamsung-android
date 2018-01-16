package de.maxisma.allaboutsamsung

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.maxisma.allaboutsamsung.articles.PostsFragment

class MainActivity : AppCompatActivity() {

    /*
     * Features TODO:
     * - Small and large widget
     * - Legal notice?
     * - Liveblog?
     * - Gallery
     * - Configuration (dark theme, push, analytics, ...)
     * - Analytics
     * - Push
     * - Featured
     * - Post overview (by category, search query, infinite scroll, pull-to-refresh, refresh onResume after a while)
     * - Post details + comments
     * - YouTube channel (notify about new videos in app)
     * - Offline posts?
     * - Give stable IDs to list items
     * - Error handling
     * - Sharing
     * - Glide preload
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
                .commitNow()
    }
}
