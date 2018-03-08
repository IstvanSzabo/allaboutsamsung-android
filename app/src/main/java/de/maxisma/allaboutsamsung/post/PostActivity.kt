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
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val TIMEOUT_MS = 20000

private const val EXTRA_POST_ID = "post_id"

fun newPostActivityIntent(context: Context, postId: PostId) = Intent(context, PostActivity::class.java).apply {
    putExtra(EXTRA_POST_ID, postId)
}

fun newPostActivityIntentTemplate(context: Context) = Intent(context, PostActivity::class.java)

fun newPostActivityFillInIntent(postId: PostId) = Intent().apply { putExtra(EXTRA_POST_ID, postId) }

class PostActivity : BaseActivity() {

    @Inject
    lateinit var appApi: AppApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent.inject(this)

        launch(UI) {
            val postId = if (intent.data != null) {
                try {
                    downloadPostId()
                } catch (e: Exception) {
                    Toast.makeText(this@PostActivity, R.string.broken_connection, Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
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
        withTimeout(TIMEOUT_MS) {
            appApi.urlToId(intent.dataString).await()
        }
    }
}