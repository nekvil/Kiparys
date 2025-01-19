package com.example.kiparys.service

import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import androidx.navigation.NavDeepLinkBuilder
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.FcmTokenRepository
import com.example.kiparys.ui.MainActivity
import com.example.kiparys.Constants.GENERAL
import com.example.kiparys.Constants.GENERAL_CHANNEL_ID
import com.example.kiparys.Constants.PROJECT_IDEAS
import com.example.kiparys.Constants.PROJECT_INVITE
import com.example.kiparys.Constants.PROJECT_MESSAGES
import com.example.kiparys.Constants.PROJECT_TASKS
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.kiparys.R


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.data.isNotEmpty()) {
            val notificationType = remoteMessage.data["notification_type"]
            val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: ""
            val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
            val imageUrl = remoteMessage.notification?.imageUrl?.toString() ?: ""
            val projectId = remoteMessage.data["project_id"] ?: ""
            val senderName = remoteMessage.data["sender_name"] ?: ""
            val senderImage = remoteMessage.data["sender_image"] ?: ""
            val channelId =
                remoteMessage.data["channel_id"] ?: remoteMessage.notification?.channelId
                ?: GENERAL_CHANNEL_ID

            when (notificationType) {
                PROJECT_INVITE -> handleProjectMessagesNotification(
                    title, body, senderName, senderImage, projectId, channelId
                )

                PROJECT_MESSAGES -> handleProjectMessagesNotification(
                    title, body, senderName, senderImage, projectId, channelId
                )

                PROJECT_TASKS -> handleProjectTasksNotification(
                    title, body, senderImage, projectId, channelId
                )

                PROJECT_IDEAS -> handleProjectIdeasNotification(
                    title, body, senderImage, projectId, channelId
                )

                GENERAL -> handleGeneralNotification(
                    title, body, imageUrl, channelId
                )
            }
        }
    }

    private fun handleProjectMessagesNotification(
        title: String, body: String, userName: String, userAvatar: String,
        projectId: String, channelId: String
    ) {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.projectDetailsFragment)
            .setArguments(Bundle().apply {
                putString("projectId", projectId)
                putString("channelId", channelId)
            })
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        val imageLoader = (applicationContext as KiparysApplication).imageLoader

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val request = ImageRequest.Builder(this)
            .data(userAvatar)
            .transformations(CircleCropTransformation())
            .precision(Precision.EXACT)
            .target { avatarDrawable ->
                val avatarBitmap = (avatarDrawable as BitmapDrawable).bitmap

                val userPerson = Person.Builder()
                    .setIcon(IconCompat.createWithBitmap(avatarBitmap))
                    .setName(userName)
                    .build()

                val style = NotificationCompat.MessagingStyle(userPerson)
                    .setConversationTitle(title)
                    .setGroupConversation(true)
                    .addMessage(body, System.currentTimeMillis(), userPerson)

                builder.setStyle(style)
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
            .build()

        imageLoader.enqueue(request)
    }

    private fun handleProjectTasksNotification(
        title: String, body: String, imageUrl: String, projectId: String, channelId: String
    ) {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.projectDetailsFragment)
            .setArguments(Bundle().apply {
                putString("projectId", projectId)
                putString("channelId", channelId)
            })
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(getString(R.string.notification_title_new_task))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)


        val imageLoader = (applicationContext as KiparysApplication).imageLoader

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val request = ImageRequest.Builder(this)
            .data(imageUrl)
            .transformations(CircleCropTransformation())
            .precision(Precision.EXACT)
            .target { imageDrawable ->
                val imageBitmap = (imageDrawable as BitmapDrawable).bitmap
                builder.setLargeIcon(imageBitmap)
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
            .build()

        imageLoader.enqueue(request)
    }

    private fun handleProjectIdeasNotification(
        title: String, body: String, imageUrl: String, projectId: String, channelId: String
    ) {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.projectDetailsFragment)
            .setArguments(Bundle().apply {
                putString("projectId", projectId)
                putString("channelId", channelId)
            })
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(getString(R.string.notification_title_new_idea))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)

        val imageLoader = (applicationContext as KiparysApplication).imageLoader
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val request = ImageRequest.Builder(this)
            .data(imageUrl)
            .transformations(CircleCropTransformation())
            .precision(Precision.EXACT)
            .target { imageDrawable ->
                val imageBitmap = (imageDrawable as BitmapDrawable).bitmap
                builder.setLargeIcon(imageBitmap)
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
            .build()

        imageLoader.enqueue(request)
    }

    private fun handleGeneralNotification(
        title: String, body: String, imageUrl: String, channelId: String
    ) {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.projectsFragment)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(body)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        val imageLoader = (applicationContext as KiparysApplication).imageLoader
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val request = ImageRequest.Builder(this)
            .data(imageUrl)
            .precision(Precision.EXACT)
            .target { imageDrawable ->
                val imageBitmap = (imageDrawable as BitmapDrawable).bitmap
                builder.setLargeIcon(imageBitmap)
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(imageBitmap)
                        .bigLargeIcon(null as Bitmap?)
                )
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
            .build()

        imageLoader.enqueue(request)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        val authRepository = AuthRepository()
        val userId = authRepository.getCurrentUserId()

        if (userId != null) {
            saveTokenToServer(token, userId)
        } else {
            Log.d(TAG, "User is not authenticated, token not saved.")
        }
    }

    private fun saveTokenToServer(token: String, userId: String) {
        val fcmTokenRepository = FcmTokenRepository()
        CoroutineScope(Dispatchers.IO).launch {
            val result = fcmTokenRepository.setToken(userId, token)
            result.onSuccess {
                Log.d(TAG, "Token successfully saved for user: $userId")
            }.onFailure { error ->
                Log.e(TAG, "Error saving token to server: ${error.message}", error)
            }
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMessagingService"
    }
}
