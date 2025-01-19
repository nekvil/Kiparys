package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserProject @Keep constructor(
    val name: String? = null,
    val projectImageUrl: String? = null,
    val lastMessage: String? = null,
    val senderId: String? = null,
    val draft: String? = null,
    val chatPosition: Int? = null,
    val timestamp: Long? = null,
    val lastSeenTimestamp: Long? = null,
    val unreadMessagesCount: Int? = null,
    val pinned: Boolean? = null,
    val notifications: Boolean? = null,

    @get:Exclude
    var id: String? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "draft" to draft,
            "senderId" to senderId,
            "chatPosition" to chatPosition,
            "projectImageUrl" to projectImageUrl,
            "lastMessage" to lastMessage,
            "timestamp" to timestamp,
            "lastSeenTimestamp" to lastSeenTimestamp,
            "unreadMessagesCount" to unreadMessagesCount,
            "notifications" to notifications,
            "pinned" to pinned
        ).filterValues { it != null }
    }
}
