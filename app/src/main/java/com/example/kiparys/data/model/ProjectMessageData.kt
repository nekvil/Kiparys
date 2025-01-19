package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ProjectMessage @Keep constructor(
    val senderId: String? = null,
    val senderName: String? = null,
    val senderImageUrl: String? = null,
    val content: String? = null,
    val timestamp: Long? = null,
    val pinned: Boolean? = null,
    val edited: Boolean? = null,
    val seen: Boolean? = null,
    val replyTo: ReplyInfo? = null,
    val media: Map<String, MediaMetadata>? = null,
    val file: FileMetadata? = null,

    @get:Exclude
    var id: String? = null,
    @Exclude
    var showName: Boolean = false,
    @Exclude
    var showTime: Boolean = false,
    @Exclude
    var showAvatar: Boolean = false,
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "senderImageUrl" to senderImageUrl,
            "content" to content,
            "timestamp" to timestamp,
            "pinned" to pinned,
            "seen" to seen,
            "edited" to edited,
            "replyTo" to replyTo,
            "media" to media,
            "file" to file,
        ).filterValues { it != null }
    }
}

@Keep
@IgnoreExtraProperties
data class ReplyInfo @Keep constructor(
    val replyMessageId: String? = null,
    val replySenderId: String? = null,
    val replySenderImageUrl: String? = null,
    val replySenderName: String? = null,
    val replyContent: String? = null,
    val album: Boolean? = null,
    val file: Boolean? = null,
    val image: Boolean? = null,
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "replyMessageId" to replyMessageId,
            "replySenderId" to replySenderId,
            "replySenderImageUrl" to replySenderImageUrl,
            "replySenderName" to replySenderName,
            "replyContent" to replyContent,
            "album" to album,
            "file" to file,
            "image" to image,
        ).filterValues { it != null }
    }
}

@Keep
@IgnoreExtraProperties
data class FileMetadata @Keep constructor(
    val fileId: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val size: Long? = null,
    val fileUrl: String? = null,
    val loading: Boolean? = null,
    val uploaded: Long? = null,
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "fileId" to fileId,
            "fileName" to fileName,
            "mimeType" to mimeType,
            "fileUrl" to fileUrl,
            "loading" to loading,
            "uploaded" to uploaded,
            "size" to size
        ).filterValues { it != null }
    }
}

@Keep
@IgnoreExtraProperties
data class MediaMetadata @Keep constructor(
    val tempMediaUrl: String? = null,
    val mimeType: String? = null,
    val aspectRatio: Float? = null,
    val mediaUrl: String? = null,
    val uploaded: Long? = null,
    val loading: Boolean? = null,

    @get:Exclude
    var id: String? = null,
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "aspectRatio" to aspectRatio,
            "mediaUrl" to mediaUrl,
            "tempMediaUrl" to tempMediaUrl,
            "mimeType" to mimeType,
            "uploaded" to uploaded,
            "loading" to loading
        ).filterValues { it != null }
    }
}
