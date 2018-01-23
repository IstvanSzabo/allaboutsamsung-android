package de.maxisma.allaboutsamsung.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val guid = message.data["guid"] ?: return
        val categories = message.data["categories"] ?: return
        val tags = message.data["tags"] ?: return

        if (/* not interested in categories or tags */ false) {
            // Unsubscribe from those (categories and tags are JSON arrays)
        } else {
            // TODO JobDispatcher
            // TODO Fetch post, notify
        }
    }
}