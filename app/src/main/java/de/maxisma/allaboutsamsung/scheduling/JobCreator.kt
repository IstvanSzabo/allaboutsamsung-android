package de.maxisma.allaboutsamsung.scheduling

import com.evernote.android.job.JobCreator
import de.maxisma.allaboutsamsung.appwidget.WIDGET_JOB_TAG
import de.maxisma.allaboutsamsung.appwidget.WidgetJob

class JobCreator : JobCreator {
    override fun create(tag: String) = when (tag) {
        NOTIFICATION_JOB_TAG -> NotificationJob()
        WIDGET_JOB_TAG -> WidgetJob()
        else -> null
    }
}