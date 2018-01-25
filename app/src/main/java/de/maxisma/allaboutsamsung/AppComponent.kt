package de.maxisma.allaboutsamsung

import android.content.Context
import dagger.Component
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.post.PostFragment
import de.maxisma.allaboutsamsung.post.PostHtmlGenerator
import de.maxisma.allaboutsamsung.posts.PostsFragment
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.settings.PreferenceFragment
import de.maxisma.allaboutsamsung.settings.PreferenceHolder
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

    fun inject(postsFragment: PostsFragment)
    fun inject(postFragment: PostFragment)
    fun inject(preferenceFragment: PreferenceFragment)
}