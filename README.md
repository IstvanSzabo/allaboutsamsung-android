# AllAboutSamsung for Android

This app is a client for [AllAboutSamsung.de](https://allaboutsamsung.de/). Although it is currently specific to that website, it can be easily adapted for other WordPress-based blogs as well. Most configuration is done through the `app/build.gradle`.

To insert ads into posts, see the API WordPress needs to be implement in `AppApi.kt`. Here you will also find another API that needs to be implemented in order to also handle URL intents for the website.

Push notifications are handled using Firebase Cloud Messaging. In order to support this, a WordPress website needs to let Firebase know about each new post and its categories and tags which are mapped to Firebase Topics. See the `.notification` package for details, in particular `MessagingService` to see the data the app expects to receive from Firebase.
