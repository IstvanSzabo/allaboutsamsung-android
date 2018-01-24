package de.maxisma.allaboutsamsung.scheduling

import com.evernote.android.job.JobCreator

class JobCreator : JobCreator {
    override fun create(tag: String) = when (tag) {
        NOTIFICATION_JOB_TAG -> NotificationJob()
        else -> null
    }
}