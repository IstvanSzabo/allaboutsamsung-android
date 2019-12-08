package de.maxisma.allaboutsamsung.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.maxisma.allaboutsamsung.BaseActivity
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.CategoryId
import de.maxisma.allaboutsamsung.db.CategorySubscription
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.TagId
import de.maxisma.allaboutsamsung.db.TagSubscription
import de.maxisma.allaboutsamsung.notification.updatePushSubscription
import de.maxisma.allaboutsamsung.utils.DbWriteDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

fun newPreferencesActivityIntent(context: Context) = Intent(context, PreferenceActivity::class.java)

class PreferenceActivity : BaseActivity(useDefaultMenu = false) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, PreferenceFragment())
            .commit()
    }
}

/**
 * Check the user's notification preferences and update Firebase topic subscriptions
 */
fun PreferenceHolder.updatePushSubscriptionsAccordingly(db: Db) {
    GlobalScope.launch(DbWriteDispatcher) {
        val pushCategories: Set<CategoryId> = when (pushTopics) {
            PushTopics.NONE, PushTopics.ALL -> emptySet()
            PushTopics.BREAKING -> setOf(BuildConfig.BREAKING_CATEGORY_ID)
        }
        val pushTags: Set<TagId> = if (pushDeals) setOf(BuildConfig.DEAL_TAG_ID) else emptySet()
        val wildcard = pushTopics == PushTopics.ALL

        db.categoryDao.replaceCategorySubscriptions(pushCategories.map { CategorySubscription(it) })
        db.tagDao.replaceTagSubscriptions(pushTags.map { TagSubscription(it) })

        updatePushSubscription(db, wildcard)
    }
}

/**
 * Shows [R.xml.preferences] and updates push topic subscriptions
 * whenever notification settings change
 */
class PreferenceFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var db: Db

    @Inject
    lateinit var preferenceHolder: PreferenceHolder

    private val listener = {
        preferenceHolder.updatePushSubscriptionsAccordingly(db)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        activity!!.app.appComponent.inject(this)

        setPreferencesFromResource(R.xml.preferences, rootKey)
        preferenceHolder.registerListener(listener)
    }

    override fun onStop() {
        preferenceHolder.unregisterListener(listener)
        super.onStop()
    }
}