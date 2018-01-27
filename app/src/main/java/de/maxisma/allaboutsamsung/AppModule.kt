package de.maxisma.allaboutsamsung

import android.arch.persistence.room.Room
import android.content.Context
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import dagger.Module
import dagger.Provides
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.post.AndroidPostHtmlGenerator
import de.maxisma.allaboutsamsung.post.PostHtmlGenerator
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.rest.wordpressApi as retrofitWordpressApi

@Module
class AppModule(private val app: App) {
    @Provides
    fun app(): App = app

    @Provides
    fun context(): Context = app

    @Provides
    fun room(): Db = Room.databaseBuilder(app, Db::class.java, "db").build()

    @Provides
    fun wordpressApi(): WordpressApi = retrofitWordpressApi

    @Provides
    fun postHtmlGenerator(): PostHtmlGenerator = AndroidPostHtmlGenerator(app)

    @Provides
    fun youTube(): YouTube {
        val httpTransport = NetHttpTransport()
        val httpRequestFactory = httpTransport.createRequestFactory()
        val jsonFactory = JacksonFactory()
        val httpRequestInitializer = httpRequestFactory.initializer
        return YouTube.Builder(httpTransport, jsonFactory, httpRequestInitializer).build()
    }
}