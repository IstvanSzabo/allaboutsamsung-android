package de.maxisma.allaboutsamsung

import android.arch.persistence.room.Room
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.wordpressApi
import de.maxisma.allaboutsamsung.utils.IOPool
import kotlinx.coroutines.experimental.launch

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
     *
     * - Keep posts, tags, categories in an observable DB, observe that in ViewModels
     *
     * - DEPENDENCY INJECTION
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launch(IOPool) {
            // TODO Inject this
            val db = Room.databaseBuilder(applicationContext, Db::class.java, "db").build()
            val query = Query.Filter("HTC", null, null)
            val queryExecutor = query.newExecutor(wordpressApi, db)

            val data = queryExecutor.data.observeForever {
                queryExecutor.let {
                    Unit
                }
                println(it)
            }
            queryExecutor.requestNewerPosts()
        }
    }
}
