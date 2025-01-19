package com.example.kiparys.service

import com.example.kiparys.data.model.NotificationPayload
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface MessagingService {
    @POST("/api/send-notification")
    suspend fun sendNotification(@Body payload: NotificationPayload): Response<Unit>
}
