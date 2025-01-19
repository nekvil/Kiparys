package com.example.kiparys.data.model

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonInclude


@Keep
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DataManagementPayload @Keep constructor(
    val users: Set<String>? = null,
    val userId: String? = null,
    val userName: String? = null,
    val projectId: String? = null,
    val messageId: String? = null,
    val content: String? = null,
    val lastMessageContent: String? = null
)
