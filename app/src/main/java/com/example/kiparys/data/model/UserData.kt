package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class User @Keep constructor(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val unverifiedEmail: String? = null,
    val profileImageUrl: String? = null,
    val birthdate: Long? = null,
    val lastOnline: Long? = null,
    val connections: Map<String, Long>? = null,
    val about: String? = null,
    val deleted: Boolean? = null,
    val profileImageUpdating: Boolean? = null,
    val nameUpdating: Boolean? = null,

    @get:Exclude
    var id: String? = null
) {

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "unverifiedEmail" to unverifiedEmail,
            "profileImageUrl" to profileImageUrl,
            "birthdate" to birthdate,
            "lastOnline" to lastOnline,
            "about" to about,
            "deleted" to deleted
        ).filterValues { it != null }
    }
}
