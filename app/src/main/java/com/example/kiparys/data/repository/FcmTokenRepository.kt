package com.example.kiparys.data.repository

import com.google.firebase.Firebase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.database
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await

class FcmTokenRepository {

    private val database = Firebase.database
    private val messaging = Firebase.messaging

    suspend fun getToken(): Result<String> {
        return try {
            val token = messaging.token.await()
            Result.success(token)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun setToken(userId: String, token: String): Result<Unit> {
        return try {
            val reference = database.reference.child("fcmTokens/$userId")
            val tokenReference = reference.child(token)
            tokenReference.setValue(ServerValue.TIMESTAMP).await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

}
