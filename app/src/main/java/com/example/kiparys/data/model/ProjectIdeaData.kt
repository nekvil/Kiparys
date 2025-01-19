package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ProjectIdea @Keep constructor(
    val description: String? = null,
    val imageUrl: String? = null,
    val created: Long? = null,
    val votes: Map<String, Boolean>? = null,

    @get:Exclude
    var id: String? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "description" to description,
            "imageUrl" to imageUrl,
            "created" to created,
            "votes" to votes
        ).filterValues { it != null }
    }
}
