package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonInclude


@Keep
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NotificationPayload @Keep constructor(
    val users: Set<String>,
    val notificationType: String,
    val title: String,
    val body: String,
    val channelId: String,
    val timestamp: Long? = null,
    val projectId: String? = null,
    val ideaId: String? = null,
    val taskId: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val senderImage: String? = null,
    val imageUrl: String? = null,
    val priority: String? = null
)
