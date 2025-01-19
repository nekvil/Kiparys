package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ProjectFile @Keep constructor(
    val loading: Boolean? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val fileUrl: String? = null,
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
            "fileUrl" to fileUrl,
            "fileName" to fileName,
            "mimeType" to mimeType,
            "projectMessageId" to projectMessageId,
            "uploaderId" to uploaderId,
            "uploaderName" to uploaderName,
            "uploaderImageUrl" to uploaderImageUrl,
            "uploaded" to uploaded,
            "size" to size,
            "loading" to loading
        ).filterValues { it != null }
    }
}
