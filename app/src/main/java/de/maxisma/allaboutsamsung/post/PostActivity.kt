package de.maxisma.allaboutsamsung.post

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import de.maxisma.allaboutsamsung.BaseActivity
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.rest.AppApi
import de.maxisma.allaboutsamsung.rest.urlToId
import de.maxisma.allaboutsamsung.utils.retry
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val EXTRA_POST_ID = "post_id"

/**
 * Creates an [Intent] for displaying the specified post.
 */
fun newPostActivityIntent(context: Context, postId: PostId) = Intent(context, PostActivity::class.java).apply {
    putExtra(EXTRA_POST_ID, postId)
}

/**
 * Empty [Intent] to be used as a template for PendingIntents in the posts appwidget.
 *
 * @see newPostActivityFillInIntent
 */
fun newPostActivityIntentTemplate(context: Context) = Intent(context, PostActivity::class.java)

/**
 * Intent to fill the template from [newPostActivityIntentTemplate]
 */
fun newPostActivityFillInIntent(postId: PostId) = Intent().apply { putExtra(EXTRA_POST_ID, postId) }

class PostActivity : BaseActivity() {

    @Inject
    lateinit var appApi: AppApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent.inject(this)

        uiLaunch {
            val postId = if (intent.data != null) {
                try {
                    downloadPostId()
                } catch (e: Exception) {
                    Toast.makeText(this@PostActivity, R.string.broken_connection, Toast.LENGTH_LONG).show()
                    finish()
                    return@uiLaunch
                }
            } else {
                intent.getLongExtra(EXTRA_POST_ID, -1)
            }


            val fragment = PostFragment(postId)
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }

    private suspend fun downloadPostId() = retry(
        HttpException::class,
        JsonDataException::class,
        JsonEncodingException::class,
        IOException::class,
        TimeoutCancellationException::class
    ) {
        appApi.urlToId(intent.dataString ?: error("Intent did not contain expected dataString"))
    }
}