package de.maxisma.allaboutsamsung.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.newMainActivityIntent
import de.maxisma.allaboutsamsung.post.newPostActivityIntentTemplate

class PostsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            val intent = newPostActivityIntentTemplate(context)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val serviceIntent = Intent(context, PostsWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val headerIntent = newMainActivityIntent(context)
            val headerPendingIntent = PendingIntent.getActivity(context, 0, headerIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val views = RemoteViews(context.packageName, R.layout.posts_widget)
            views.setPendingIntentTemplate(R.id.postsWidgetListView, pendingIntent)
            views.setRemoteAdapter(R.id.postsWidgetListView, serviceIntent)
            views.setOnClickPendingIntent(R.id.postsWidgetHeader, headerPendingIntent)
            views.setEmptyView(R.id.postsWidgetListView, R.id.postsWidgetEmptyView)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.scheduleWidgetJob()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.unscheduleWidgetJob()
    }
}