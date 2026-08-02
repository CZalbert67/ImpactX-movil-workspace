package com.example.impactx.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    fun refresh(
        @Body request: RefreshTokenRequest
    ): retrofit2.Call<AuthResponse> // Use Call synchronally for OkHttp Authenticator block

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>

    @PUT("api/v1/devices/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body request: UpdateFcmTokenRequest
    ): Response<Unit>

    @GET("api/v1/vehicles")
    suspend fun getVehicles(): Response<List<VehicleDto>>

    @POST("api/v1/vehicles")
    suspend fun createVehicle(
        @Body request: CreateVehicleRequest
    ): Response<VehicleDto>
}
