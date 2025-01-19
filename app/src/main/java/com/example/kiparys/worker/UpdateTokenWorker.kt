package com.example.kiparys.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.FcmTokenRepository

class UpdateTokenWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val fcmTokensRepository: FcmTokenRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = authRepository.getCurrentUserId()
            val fcmToken = getToken() ?: return Result.failure()

            return if (userId != null) {
                setToken(userId, fcmToken)
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during token update: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun getToken(): String? {
        val tokenResult = fcmTokensRepository.getToken()
        return if (tokenResult.isSuccess) {
            tokenResult.getOrNull()
        } else {
            null
        }
    }

    private suspend fun setToken(userId: String, fcmToken: String): Result {
        val saveTokenResult = fcmTokensRepository.setToken(fcmToken, userId)
        return if (saveTokenResult.isSuccess) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "UpdateTokenWorker"
    }
}
