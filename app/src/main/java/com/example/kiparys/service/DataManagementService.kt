package com.example.kiparys.service

import com.example.kiparys.data.model.DataManagementPayload
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface DataManagementService {
    @POST("/api/user/update/name")
    suspend fun updateUsername(@Body payload: DataManagementPayload): Response<Unit>

    @POST("/api/user/update/image-url")
    suspend fun updateProfileImageUrl(@Body payload: DataManagementPayload): Response<Unit>

    @POST("/api/projectMessages/update/message")
    suspend fun updateProjectMessage(@Body payload: DataManagementPayload): Response<Unit>
}
