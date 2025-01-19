package com.example.kiparys.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import com.example.kiparys.Constants
import com.example.kiparys.R


class NotificationChannelsManager(private val context: Context) {

    fun createNotificationChannels() {
        val projectMessagesChannel = NotificationChannel(
            Constants.PROJECT_MESSAGES_CHANNEL_ID,
            context.getString(R.string.notification_channel_name_project_messages),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                context.getString(R.string.notification_channel_description_project_messages)
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 100, 200)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val projectTasksChannel = NotificationChannel(
            Constants.PROJECT_TASKS_CHANNEL_ID,
            context.getString(R.string.notification_channel_name_project_tasks),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description_project_tasks)
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 150, 300)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val projectIdeasChannel = NotificationChannel(
            Constants.PROJECT_IDEAS_CHANNEL_ID,
            context.getString(R.string.notification_channel_name_project_ideas),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description_project_ideas)
            enableLights(true)
            lightColor = Color.YELLOW
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 100, 200)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val generalChannel = NotificationChannel(
            Constants.GENERAL_CHANNEL_ID,
            context.getString(R.string.notification_channel_name_general),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description_general)
            enableLights(false)
            enableVibration(false)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(
            listOf(projectMessagesChannel, projectTasksChannel, projectIdeasChannel, generalChannel)
        )
    }
}
