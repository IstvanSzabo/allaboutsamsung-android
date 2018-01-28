package de.maxisma.allaboutsamsung

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.post.newPostActivityIntent
import de.maxisma.allaboutsamsung.posts.PostsFragment
import de.maxisma.allaboutsamsung.settings.updatePushSubscriptionsAccordingly
import de.maxisma.allaboutsamsung.youtube.YouTubeFragment
import kotlinx.android.synthetic.main.activity_main.*

fun newMainActivityIntent(context: Context) = Intent(context, MainActivity::class.java)

class MainActivity : BaseActivity(), PostsFragment.InteractionListener, YouTubeFragment.InteractionListener {

    /*
     * Features TODO:
     * - Small and large widget
     * - Remaining configuration options, honor theme + analytics
     * - Analytics (article openings etc.)
     * - Featured
     * - Refresh in onResume after a while
     * - Test offline access
     * - Ads
     * - Resolve warnings, code inspections
     * - Documentation
     * - Test on older versions
     * - Automatic settings migration and/or setup wizard
     * - Rotation
     * - Send app version header
     * - Check for memory leaks
     * - Expire old data, that is also refresh when scrolling down
     * - Integrate icon in toolbar?
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        app.appComponent.preferenceHolder.updatePushSubscriptionsAccordingly(app.appComponent.db)

        mainViewPager.adapter = MainAdapter(this, supportFragmentManager)
        mainTabLayout.setupWithViewPager(mainViewPager, true)
    }

    override fun displayPost(postId: PostId) {
        startActivity(newPostActivityIntent(this, postId))
    }

    override fun notifyUnseenVideos(howMany: Int) {
        Snackbar.make(mainRoot, resources.getQuantityString(R.plurals.new_videos, howMany), Snackbar.LENGTH_LONG).show()
    }
}

private class MainAdapter(private val context: Context, fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {
    override fun getItem(position: Int) = when (position) {
        0 -> PostsFragment()
        1 -> YouTubeFragment()
        else -> throw IndexOutOfBoundsException("Unknown position $position.")
    }

    override fun getPageTitle(position: Int): CharSequence = when (position) {
        0 -> context.getString(R.string.posts)
        1 -> context.getString(R.string.videos)
        else -> throw IndexOutOfBoundsException("Unknown position $position.")
    }

    override fun getCount() = 2
}
