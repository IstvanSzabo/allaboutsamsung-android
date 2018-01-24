package de.maxisma.allaboutsamsung.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.annotation.WorkerThread
import android.support.v4.app.NotificationCompat
import com.evernote.android.job.Job
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.mainActivityIntent
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.utils.IOPool
import de.maxisma.allaboutsamsung.utils.glide.GlideApp
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jsoup.Jsoup
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore

@WorkerThread
fun notifyAboutPost(postId: PostId, db: Db, api: WordpressApi, context: Context): Job.Result {
    val barrier = Semaphore(0)
    var result = Job.Result.SUCCESS

    fun reschedule(e: Exception) {
        e.printStackTrace()
        result = Job.Result.RESCHEDULE
        barrier.release()
    }

    val query = Query.Filter(string = null, onlyCategories = null, onlyTags = null, onlyIds = listOf(postId))
    val executor = query.newExecutor(api, db, onError = ::reschedule)
    launch(UI) {
        executor.requestNewerPosts().join()

        val value = executor.data.value ?: return@launch run { reschedule(Exception("Download of post failed!")) }
        launch(IOPool) {
            try {
                val vm = value.single().toNotificationViewModel(context)
                vm.notifyAboutPost(context)
                barrier.release()
            } catch (e: ExecutionException) {
                reschedule(e)
            }
        }.join()
    }

    barrier.acquire()
    return result
}

private data class PostNotificationViewModel(val post: Post, val image: Bitmap, val textContent: String)

@WorkerThread
@Throws(ExecutionException::class)
private fun Post.toNotificationViewModel(context: Context): PostNotificationViewModel {
    val bitmap = GlideApp.with(context)
        .asBitmap()
        .load(imageUrl)
        .submit()
        .get()
    val textContent = Jsoup.parse(content).text()
    return PostNotificationViewModel(this, bitmap, textContent)
}

private fun PostNotificationViewModel.notifyAboutPost(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNewsNotificationChannel(context)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        mainActivityIntent(context, post.id),
        PendingIntent.FLAG_CANCEL_CURRENT
    )
    val notification = NotificationCompat.Builder(context, NEWS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO right icon
        .setContentTitle(post.title)
        .setContentText(textContent)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setStyle(NotificationCompat.BigPictureStyle().bigPicture(image))
        .build()
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(post.id.toInt(), notification)
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