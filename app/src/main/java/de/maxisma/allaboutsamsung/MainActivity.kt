package de.maxisma.allaboutsamsung

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.post.newPostActivityIntent
import de.maxisma.allaboutsamsung.posts.PostsFragment
import de.maxisma.allaboutsamsung.settings.updatePushSubscriptionsAccordingly
import de.maxisma.allaboutsamsung.utils.DbWriteDispatcher
import de.maxisma.allaboutsamsung.youtube.YouTubeFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.Date
import javax.inject.Inject

private val oldestAllowedDbPostCreatedDate =
    Date(System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000)

fun newMainActivityIntent(context: Context) = Intent(context, MainActivity::class.java)

class MainActivity : BaseActivity(), PostsFragment.InteractionListener, YouTubeFragment.InteractionListener {

    /*
     * Features TODO:
     * - Resolve warnings, code inspections
     * - Documentation
     * - Test on older versions
     * - Rotation
     * - Check for memory leaks
     * - Tablet UI
     * - Look into why search may have no effect in some situations
     * - Fix notification logo tint on Samsung? Fix implemented, to be tested.
     */

    @Inject
    lateinit var db: Db

    override val darkThemeToUse = R.style.AppTheme_NoActionBar_Dark

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        app.appComponent.inject(this)

        launch(UI) {
            launch(DbWriteDispatcher) { db.postDao.deleteExpired(oldestAllowedDbPostCreatedDate) }.join()
            preferenceHolder.updatePushSubscriptionsAccordingly(app.appComponent.db)

            mainViewPager.adapter = MainAdapter(this@MainActivity, supportFragmentManager)
            mainTabLayout.setupWithViewPager(mainViewPager, true)
        }
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
