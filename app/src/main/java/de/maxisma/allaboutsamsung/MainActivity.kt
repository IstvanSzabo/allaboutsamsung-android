package de.maxisma.allaboutsamsung

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.View
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.post.PostFragment
import de.maxisma.allaboutsamsung.post.newPostActivityIntent
import de.maxisma.allaboutsamsung.posts.PostsFragment
import de.maxisma.allaboutsamsung.settings.updatePushSubscriptionsAccordingly
import de.maxisma.allaboutsamsung.utils.DbWriteDispatcher
import de.maxisma.allaboutsamsung.youtube.YouTubeFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_common.*
import kotlinx.coroutines.experimental.launch
import java.util.Date
import javax.inject.Inject

private val oldestAllowedDbPostCreatedDate =
    Date(System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000)

private const val STATE_POST_ID = "post_id"

fun newMainActivityIntent(context: Context) = Intent(context, MainActivity::class.java)

class MainActivity : BaseActivity(), PostsFragment.InteractionListener, YouTubeFragment.InteractionListener {

    // TODO Look into why search may have no effect in some situations

    @Inject
    lateinit var db: Db

    private var displayedPostId: PostId? = null

    override val darkThemeToUse = R.style.AppTheme_Dark

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayedPostId = savedInstanceState
            ?.getLong(STATE_POST_ID, -1)
            .let { if (it == -1L) null else it }

        setContentView(R.layout.activity_main)
        supportActionBar?.elevation = 0f

        app.appComponent.inject(this)

        uiLaunch {
            launch(DbWriteDispatcher) { db.postDao.deleteExpired(oldestAllowedDbPostCreatedDate) }.join()
            preferenceHolder.updatePushSubscriptionsAccordingly(app.appComponent.db)

            mainViewPager.adapter = MainAdapter(this@MainActivity, supportFragmentManager)
            mainTabLayout.setupWithViewPager(mainViewPager, true)

            val displayedPostId = displayedPostId
            if (postFragmentContainer != null && savedInstanceState != null && displayedPostId != null) {
                displayPost(displayedPostId)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_POST_ID, displayedPostId ?: -1)
    }

    override fun onDisplayedPostsChanged(posts: List<Post>) {
        if (postFragmentContainer != null && displayedPostId == null && posts.isNotEmpty()) {
            displayPost(posts[0].id)
        }
    }

    override fun displayPost(postId: PostId) {
        displayedPostId = postId

        if (postFragmentContainer != null) {
            postFragmentContainerProgressBar!!.visibility = View.GONE
            supportFragmentManager.beginTransaction()
                .replace(R.id.postFragmentContainer, PostFragment(postId))
                .commit()
        } else {
            startActivity(newPostActivityIntent(this, postId))
        }
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
