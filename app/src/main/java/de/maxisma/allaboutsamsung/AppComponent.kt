package de.maxisma.allaboutsamsung

import android.content.Context
import com.google.api.services.youtube.YouTube
import dagger.Component
import de.maxisma.allaboutsamsung.appwidget.PostsWidgetRemoteViewsFactory
import de.maxisma.allaboutsamsung.categories.CategoryActivity
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.post.PostActivity
import de.maxisma.allaboutsamsung.post.PostFragment
import de.maxisma.allaboutsamsung.post.html.PostHtmlGenerator
import de.maxisma.allaboutsamsung.posts.PostsFragment
import de.maxisma.allaboutsamsung.rest.AppApi
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.settings.PreferenceFragment
import de.maxisma.allaboutsamsung.settings.PreferenceHolder
import de.maxisma.allaboutsamsung.youtube.YouTubeFragment
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Component(modules = [(AppModule::class)])
@Singleton
interface AppComponent {
    val db: Db
    val context: Context
    val app: App
    val wordpressApi: WordpressApi
    val postHtmlGenerator: PostHtmlGenerator
    val preferenceHolder: PreferenceHolder
    val youTube: YouTube
    val httpClient: OkHttpClient
    val appApi: AppApi

    fun inject(postsFragment: PostsFragment)
    fun inject(postFragment: PostFragment)
    fun inject(preferenceFragment: PreferenceFragment)
    fun inject(categoryActivity: CategoryActivity)
    fun inject(youTubeFragment: YouTubeFragment)
    fun inject(baseActivity: BaseActivity)
    fun inject(postActivity: PostActivity)
    fun inject(postsWidgetRemoteViewsFactory: PostsWidgetRemoteViewsFactory)
}