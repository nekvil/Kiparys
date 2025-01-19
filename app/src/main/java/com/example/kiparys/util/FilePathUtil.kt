package com.example.kiparys.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


object FilePathUtil {
    private fun generateTimeStamp(): String {
        val dateFormat = SimpleDateFormat("ddMMyyyy-HHmmss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun generateUniqueId(): String {
        return UUID.randomUUID().toString()
    }

    enum class FileType {
        USER_PROFILE_IMAGE_PLACEHOLDER,
        PROJECT_PROFILE_IMAGE_PLACEHOLDER,

        USER_PROFILE_IMAGE_UPLOAD,
        PROJECT_PROFILE_IMAGE_UPLOAD,

        PROJECT_CHAT_MEDIA_ATTACHMENT_UPLOAD,
        PROJECT_CHAT_FILE_ATTACHMENT_UPLOAD,
        PROJECT_CHAT_AUDIO_ATTACHMENT_UPLOAD,

        PROJECT_IDEA_MEDIA_ATTACHMENT_UPLOAD,
    }

    fun generateFilePath(
        fileType: FileType,
        userId: String? = null,
        projectId: String? = null,
        extension: String? = null,
        name: String? = null
    ): String {
        val timeStamp = generateTimeStamp()
        val uniqueId = generateUniqueId()

        return when (fileType) {
            FileType.USER_PROFILE_IMAGE_PLACEHOLDER -> "/users/$userId/profileImage/profile-image-placeholder-${userId}-${timeStamp}-${uniqueId}.jpg"
            FileType.USER_PROFILE_IMAGE_UPLOAD -> "/users/$userId/profileImage/profile-image-upload-${userId}-${timeStamp}-${uniqueId}.jpg"

            FileType.PROJECT_PROFILE_IMAGE_PLACEHOLDER -> "/projects/$projectId/profileImage/profile-image-placeholder-${projectId}-${timeStamp}-${uniqueId}.jpg"
            FileType.PROJECT_PROFILE_IMAGE_UPLOAD -> "/projects/$projectId/profileImage/profile-image-upload-${projectId}-${timeStamp}-${uniqueId}.jpg"

            FileType.PROJECT_CHAT_MEDIA_ATTACHMENT_UPLOAD -> "/projects/$projectId/attachments/chat/media/${projectId}-${timeStamp}-${uniqueId}.$extension"
            FileType.PROJECT_CHAT_FILE_ATTACHMENT_UPLOAD -> "/projects/$projectId/attachments/chat/files/${projectId}-${timeStamp}-${uniqueId}-$name"
            FileType.PROJECT_CHAT_AUDIO_ATTACHMENT_UPLOAD -> "/projects/$projectId/attachments/chat/audio/${projectId}-${timeStamp}-${uniqueId}.$extension"

            FileType.PROJECT_IDEA_MEDIA_ATTACHMENT_UPLOAD -> "/projects/$projectId/attachments/ideas/media/${projectId}-${timeStamp}-${uniqueId}.$extension"
        }
    }

    fun isDocumentType(mimeType: String): Boolean {
        return mimeType.startsWith("application")
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName = "unknown"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

}
