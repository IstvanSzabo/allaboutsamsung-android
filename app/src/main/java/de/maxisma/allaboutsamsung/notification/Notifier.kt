package de.maxisma.allaboutsamsung.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.ListenableWorker
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.crashlytics.android.Crashlytics
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.newMainActivityIntent
import de.maxisma.allaboutsamsung.post.newPostActivityIntent
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.utils.IOPool
import de.maxisma.allaboutsamsung.utils.ellipsize
import de.maxisma.allaboutsamsung.utils.glide.GlideApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.concurrent.ExecutionException

/**
 * Fetch the post for the [postId] into the DB and shows a notification for it.
 */
suspend fun notifyAboutPost(postId: PostId, db: Db, api: WordpressApi, context: Context, keyValueStore: KeyValueStore): ListenableWorker.Result {
    var result = ListenableWorker.Result.success()

    fun reschedule(e: Exception) {
        e.printStackTrace()
        result = ListenableWorker.Result.retry()
    }

    val query = Query.Filter(onlyIds = listOf(postId))
    val value = withContext(Dispatchers.Main) {
        val executor = query.newExecutor(api, db, keyValueStore, coroutineScope = this, onError = ::reschedule)
        executor.requestNewerPosts().join()
        executor.data.value
    }

    if (value != null) {
        withContext(IOPool) {
            try {
                val vm = value.singleOrNull()?.toNotificationViewModel(context) ?: return@withContext run {
                    Crashlytics.logException(Exception("Could not find post with id $postId"))
                    // No reschedule since this error is believed to only occur when a wrong
                    // postId is received due to a server error.
                }
                vm.notifyAboutPost(context)
            } catch (e: ExecutionException) {
                e.printStackTrace()
                result = ListenableWorker.Result.retry()
            }
        }
    } else {
        Exception("Download of post failed!").printStackTrace()
        result = ListenableWorker.Result.retry()
    }

    return result
}

private data class PostNotificationViewModel(val post: Post, val image: Bitmap, val textContent: String)

private const val BITMAP_MAX_WIDTH = 1000
private const val BITMAP_MAX_HEIGHT = 1000
private const val MAX_CONTENT_LENGTH = 500

/**
 * Download the image, decode and ellipsize the content.
 */
@WorkerThread
@Throws(ExecutionException::class)
private fun Post.toNotificationViewModel(context: Context): PostNotificationViewModel {
    val bitmap = GlideApp.with(context)
        .asBitmap()
        .load(imageUrl)
        .downsample(DownsampleStrategy.AT_MOST)
        .override(BITMAP_MAX_WIDTH, BITMAP_MAX_HEIGHT)
        .submit()
        .get()
    val textContent = Jsoup.parse(content).text().ellipsize(MAX_CONTENT_LENGTH)
    return PostNotificationViewModel(this, bitmap, textContent)
}

private fun PostNotificationViewModel.notifyAboutPost(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNewsNotificationChannel(context)
    }

    val stackBuilder = TaskStackBuilder.create(context).apply {
        addNextIntentWithParentStack(newMainActivityIntent(context))
        addNextIntentWithParentStack(newPostActivityIntent(context, post.id))
    }
    val pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT)
    val notification = NotificationCompat.Builder(context, NEWS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setContentTitle(post.title)
        .setContentText(textContent)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setStyle(NotificationCompat.BigPictureStyle().bigPicture(image))
        .build()
    val notificationManager = context.getSystemService<NotificationManager>()
    notificationManager?.notify(post.id.toInt(), notification)
}

private const val NEWS_CHANNEL_ID = "news"

@RequiresApi(Build.VERSION_CODES.O)
private fun createNewsNotificationChannel(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val name = context.getString(R.string.news_channel_name)
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(NEWS_CHANNEL_ID, name, importance).apply {
        description = context.getString(R.string.news_channel_description)
        enableLights(true)
        enableVibration(false)
        lightColor = Color.BLUE
    }
    notificationManager.createNotificationChannel(channel)
}