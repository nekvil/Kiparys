package com.example.kiparys.data.repository

import android.util.Log
import com.example.kiparys.Constants.ERROR_UNKNOWN
import com.example.kiparys.data.model.DataManagementPayload
import com.example.kiparys.service.DataManagementService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response


class DataManagementRepository(private val dataManagementService: DataManagementService) {

    private companion object {
        private const val TAG = "DataManagementRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    private suspend fun <T> executeWithRetries(
        action: suspend () -> Response<T>,
        errorMessage: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                val response = action()
                Log.d(TAG, response.isSuccessful.toString())
                if (response.isSuccessful) return@withContext Result.success(Unit)
                lastException =
                    Exception("$errorMessage: ${response.errorBody()?.string()}")
            } catch (e: Exception) {
                lastException = e
            }
            if (attempt < MAX_RETRY_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        Result.failure(lastException ?: Exception(ERROR_UNKNOWN))
    }

    private suspend fun updateUsername(payload: DataManagementPayload): Result<Unit> {
        return executeWithRetries(
            action = { dataManagementService.updateUsername(payload) },
            errorMessage = "Failed to update username"
        )
    }

    private suspend fun updateProfileImageUrl(payload: DataManagementPayload): Result<Unit> {
        return executeWithRetries(
            action = { dataManagementService.updateProfileImageUrl(payload) },
            errorMessage = "Failed to update profile image URL"
        )
    }

    private suspend fun updateProjectMessage(payload: DataManagementPayload): Result<Unit> {
        return executeWithRetries(
            action = { dataManagementService.updateProjectMessage(payload) },
            errorMessage = "Failed to update project message"
        )
    }

    suspend fun updateUsername(userId: String): Result<Unit> {
        val payload = DataManagementPayload(userId = userId)
        return updateUsername(payload)
    }

    suspend fun updateProfileImageUrl(userId: String): Result<Unit> {
        val payload = DataManagementPayload(userId = userId)
        return updateProfileImageUrl(payload)
    }

    suspend fun updateProjectMessage(
        userId: String,
        userName: String,
        projectId: String,
        messageId: String,
        lastMessageContent: String,
        content: String?,
        users: Set<String>
    ): Result<Unit> {
        val payload =
            DataManagementPayload(
                userId = userId,
                userName = userName,
                projectId = projectId,
                messageId = messageId,
                lastMessageContent = lastMessageContent,
                content = content,
                users = users
            )
        return updateProjectMessage(payload)
    }

}
