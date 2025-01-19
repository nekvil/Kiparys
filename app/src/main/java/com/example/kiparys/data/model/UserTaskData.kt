package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserTask @Keep constructor(
    val projectId: String? = null,
    val projectName: String? = null,
    val name: String? = null,
    val description: String? = null,
    val created: Long? = null,
    val dueDate: Long? = null,
    val completed: Boolean? = null,

    @get:Exclude
    var id: String? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "projectId" to projectId,
            "projectName" to projectName,
            "name" to name,
            "description" to description,
            "created" to created,
            "dueDate" to dueDate,
            "completed" to completed
        ).filterValues { it != null }
    }
}
