package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ProjectTask @Keep constructor(
    val name: String? = null,
    val description: String? = null,
    val created: Long? = null,
    val dueDate: Long? = null,
    val completed: Boolean? = null,
    val assignedUser: AssignedUser? = null,

    @get:Exclude
    var id: String? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "description" to description,
            "created" to created,
            "dueDate" to dueDate,
            "completed" to completed,
            "assignedUser" to assignedUser?.toMap()
        ).filterValues { it != null }
    }
}

@Keep
@IgnoreExtraProperties
data class AssignedUser @Keep constructor(
    val assignedId: String? = null,
    val assignedName: String? = null,
    val assignedImageUrl: String? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "assignedId" to assignedId,
            "assignedName" to assignedName,
            "assignedImageUrl" to assignedImageUrl
        ).filterValues { it != null }
    }
}
