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

    // ---- Trips ----
    @POST("api/v1/trips/start")
    suspend fun startTrip(
        @Body request: StartTripRequest
    ): Response<ViajeDto>

    @POST("api/v1/trips/{id}/finish")
    suspend fun finishTrip(
        @retrofit2.http.Path("id") tripId: String
    ): Response<TripActionResponse>

    @GET("api/v1/trips/active")
    suspend fun getActiveTrip(): Response<ViajeDto>

    // ---- Alerts / SOS ----
    @POST("api/v1/alerts/sos")
    suspend fun sendSos(
        @Body request: SosRequest
    ): Response<AlertStatusDto>

    @POST("api/v1/alerts/detect")
    suspend fun detectAlert(
        @Body request: DetectAlertRequest
    ): Response<AlertStatusDto>

    // ---- Quick Messages / Chat ----
    @POST("api/v1/quick-messages/send")
    suspend fun sendQuickMessage(
        @Body request: SendQuickMessageRequest
    ): Response<QuickMessageDto>

    @GET("api/v1/quick-messages/history")
    suspend fun getMessageHistory(
        @retrofit2.http.Query("otherPublicProfileId") otherPublicProfileId: String? = null
    ): Response<List<QuickMessageDto>>

    // ---- Plans ----
    @GET("api/v1/plans")
    suspend fun getPlans(): Response<List<PlanDto>>

    // ---- Medical Profile ----
    @GET("api/v1/profile/medical")
    suspend fun getMedicalProfile(): Response<MedicalProfileDto>

    @PUT("api/v1/profile/medical")
    suspend fun updateMedicalProfile(
        @Body request: UpdateMedicalProfileRequest
    ): Response<MedicalProfileDto>

    // ---- Contacts ----
    @GET("api/v1/contacts")
    suspend fun getContacts(): Response<List<ContactoDto>>

    @POST("api/v1/contacts")
    suspend fun createContact(
        @Body request: CreateContactoRequest
    ): Response<ContactoDto>

    @DELETE("api/v1/contacts/{id}")
    suspend fun deleteContact(
        @retrofit2.http.Path("id") contactId: String
    ): Response<Void>

    // ---- Monitors ----
    @GET("api/v1/monitors")
    suspend fun getMonitors(): Response<List<MonitorDto>>

    @POST("api/v1/monitors/invite")
    suspend fun inviteMonitor(
        @Body request: InviteMonitorRequest
    ): Response<MonitorDto>

    @DELETE("api/v1/monitors/{id}")
    suspend fun revokeMonitor(
        @retrofit2.http.Path("id") monitorId: String
    ): Response<Void>
}
