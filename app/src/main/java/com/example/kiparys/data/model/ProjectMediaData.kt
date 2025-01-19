package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ProjectMedia @Keep constructor(
    val tempMediaUrl: String? = null,
    val loading: Boolean? = null,
    val mediaUrl: String? = null,
    val mimeType: String? = null,
    val aspectRatio: Float? = null,
    val projectMessageId: String? = null,
    val uploaderId: String? = null,
    val uploaderName: String? = null,
    val uploaderImageUrl: String? = null,
    val uploaded: Long? = null,
    val size: Long? = null,

    @get:Exclude
    var id: String? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "mediaUrl" to mediaUrl,
            "mimeType" to mimeType,
            "aspectRatio" to aspectRatio,
            "projectMessageId" to projectMessageId,
            "uploaderId" to uploaderId,
            "uploaderName" to uploaderName,
            "uploaderImageUrl" to uploaderImageUrl,
            "uploaded" to uploaded,
            "size" to size,
            "tempMediaUrl" to tempMediaUrl,
            "loading" to loading
        ).filterValues { it != null }
    }
}
