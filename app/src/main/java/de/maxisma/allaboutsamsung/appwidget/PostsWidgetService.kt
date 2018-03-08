package de.maxisma.allaboutsamsung.appwidget

import android.content.Intent
import android.widget.RemoteViewsService

class PostsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = PostsWidgetRemoteViewsFactory(this)
}