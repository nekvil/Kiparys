package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Project @Keep constructor(
    val name: String? = null,
    val description: String? = null,
    val projectImageUrl: String? = null,
    val status: Int? = null,
    val priority: Int? = null,
    val created: Long? = null,
    val endDate: Long? = null,
    val managers: Map<String, Boolean>? = null,
    val members: Map<String, Boolean>? = null,
    val nowInChat: Map<String, Boolean>? = null,
    val typing: Map<String, String>? = null,

    @get:Exclude
    var id: String? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "description" to description,
            "projectImageUrl" to projectImageUrl,
            "status" to status,
            "priority" to priority,
            "created" to created,
            "endDate" to endDate,
            "managers" to managers,
            "members" to members
        ).filterValues { it != null }
    }
}
