package de.maxisma.allaboutsamsung.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
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
import de.maxisma.allaboutsamsung.utils.IOPool
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

fun newPreferencesActivityIntent(context: Context) = Intent(context, PreferenceActivity::class.java)

class PreferenceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction()
            .replace(android.R.id.content, de.maxisma.allaboutsamsung.settings.PreferenceFragment())
            .commit()
    }
}

fun PreferenceHolder.updatePushSubscriptionsAccordingly(db: Db) {
    launch(DbWriteDispatcher) {
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

class PreferenceFragment : PreferenceFragment() {
    @Inject
    lateinit var db: Db

    @Inject
    lateinit var preferenceHolder: PreferenceHolder

    private val listener = {
        preferenceHolder.updatePushSubscriptionsAccordingly(db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity.app.appComponent.inject(this)

        addPreferencesFromResource(R.xml.preferences)
        preferenceHolder.registerListener(listener)
    }

    override fun onStop() {
        preferenceHolder.unregisterListener(listener)
        super.onStop()
    }
}