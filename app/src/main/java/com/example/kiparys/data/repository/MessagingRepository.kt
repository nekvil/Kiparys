package com.example.kiparys.data.repository

import android.util.Log
import com.example.kiparys.Constants.ERROR_UNKNOWN
import com.example.kiparys.data.model.NotificationPayload
import com.example.kiparys.service.MessagingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


class MessagingRepository(private val messagingService: MessagingService) {

    private companion object {
        private const val TAG = "MessagingRepository"
        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 2000L
    }

    suspend fun sendNotification(payload: NotificationPayload) = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                val response = messagingService.sendNotification(payload)
                Log.d(TAG, response.isSuccessful.toString())
                if (response.isSuccessful) return@withContext Result.success(Unit)
                lastException =
                    Exception("Failed to send notification: ${response.errorBody()?.string()}")
            } catch (e: Exception) {
                lastException = e
            }
            if (attempt < MAX_RETRY_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        Result.failure(lastException ?: Exception(ERROR_UNKNOWN))
    }

    suspend fun sendNotification(
        users: Set<String>,
        notificationType: String,
        title: String,
        body: String,
        channelId: String,
        timestamp: Long? = null,
        projectId: String? = null,
        ideaId: String? = null,
        taskId: String? = null,
        senderId: String? = null,
        senderName: String? = null,
        senderImage: String? = null,
        imageUrl: String? = null,
        priority: String? = null
    ): Result<Unit> {
        val payload = NotificationPayload(
            users = users,
            notificationType = notificationType,
            title = title,
            body = body,
            timestamp = timestamp,
            channelId = channelId,
            projectId = projectId,
            ideaId = ideaId,
            taskId = taskId,
            senderId = senderId,
            senderName = senderName,
            senderImage = senderImage,
            imageUrl = imageUrl,
            priority = priority
        )

        return sendNotification(payload)
    }

}
