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

    // ---- Contacts (V1 Account Relationships) ----
    @GET("api/v1/contacts")
    suspend fun getEmergencyContacts(): Response<List<EmergencyContactDto>>

    @POST("api/v1/contacts/invitations")
    suspend fun createEmergencyContactInvitation(
        @Body request: CreateEmergencyContactInvitationRequest
    ): Response<CreateEmergencyContactInvitationResponse>

    @POST("api/v1/contacts/invitations/accept")
    suspend fun acceptEmergencyContactInvitation(
        @Body request: RespondEmergencyContactInvitationRequest
    ): Response<Unit>

    @POST("api/v1/contacts/invitations/reject")
    suspend fun rejectEmergencyContactInvitation(
        @Body request: RespondEmergencyContactInvitationRequest
    ): Response<Unit>

    @PATCH("api/v1/contacts/{id}/primary")
    suspend fun makeEmergencyContactPrimary(
        @retrofit2.http.Path("id") id: String
    ): Response<EmergencyContactDto>

    @DELETE("api/v1/contacts/{id}")
    suspend fun revokeEmergencyContact(
        @retrofit2.http.Path("id") id: String
    ): Response<Unit>

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

    // ---- Profile Username ----
    @GET("api/v1/profile/username")
    suspend fun getProfileUsername(): Response<UserProfileUsernameDto>

    // ---- Monitoring Relationships ----
    @GET("api/v1/monitoring-relationships")
    suspend fun getMonitoringRelationships(): Response<List<MonitoringRelationshipDto>>

    @POST("api/v1/monitoring-relationships/invitations")
    suspend fun createMonitoringInvitation(
        @Body request: CreateMonitoringInvitationRequest
    ): Response<CreateMonitoringInvitationResponse>

    @POST("api/v1/monitoring-relationships/invitations/accept")
    suspend fun acceptMonitoringInvitation(
        @Body request: AcceptMonitoringInvitationRequest
    ): Response<Void>

    @POST("api/v1/monitoring-relationships/invitations/reject")
    suspend fun rejectMonitoringInvitation(
        @Body request: RespondMonitoringInvitationRequest
    ): Response<Void>

    @DELETE("api/v1/monitoring-relationships/{publicRelationshipId}")
    suspend fun revokeMonitoringRelationship(
        @retrofit2.http.Path("publicRelationshipId") publicRelationshipId: String
    ): Response<Void>

    @POST("api/v1/monitoring-relationships/{publicRelationshipId}/block")
    suspend fun blockMonitoringRelationship(
        @retrofit2.http.Path("publicRelationshipId") publicRelationshipId: String
    ): Response<Void>

    // ---- Quick Messages ----
    @GET("api/v1/quick-messages/templates")
    suspend fun getQuickMessageTemplates(): Response<List<QuickMessageTemplateDto>>

    @POST("api/v1/quick-messages/templates")
    suspend fun createQuickMessageTemplate(
        @Body request: UpsertQuickMessageTemplateRequest
    ): Response<QuickMessageTemplateDto>

    @DELETE("api/v1/quick-messages/templates/{publicTemplateId}")
    suspend fun deleteQuickMessageTemplate(
        @retrofit2.http.Path("publicTemplateId") publicTemplateId: String
    ): Response<Void>

    @POST("api/v1/quick-messages/send")
    suspend fun sendQuickMessage(
        @Body request: SendQuickMessageRequest
    ): Response<QuickMessageDto>

    @GET("api/v1/quick-messages/history")
    suspend fun getQuickMessageHistory(
        @retrofit2.http.Query("otherPublicProfileId") otherPublicProfileId: String?
    ): Response<List<QuickMessageDto>>

    @PATCH("api/v1/quick-messages/{publicMessageId}/read")
    suspend fun markQuickMessageRead(
        @retrofit2.http.Path("publicMessageId") publicMessageId: String
    ): Response<Void>

    // ---- Notificaciones ----
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @retrofit2.http.Query("pageSize") pageSize: Int? = null,
        @retrofit2.http.Query("continuationToken") continuationToken: String? = null
    ): Response<List<NotificacionDto>>

    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadNotificationsCount(): Response<Map<String, Int>>

    @PATCH("api/v1/notifications/{id}/read")
    suspend fun toggleNotificationRead(
        @retrofit2.http.Path("id") id: String,
        @Body request: ToggleReadRequest
    ): Response<Map<String, String>>

    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<Map<String, String>>

    @DELETE("api/v1/notifications/{id}")
    suspend fun deleteNotification(
        @retrofit2.http.Path("id") id: String
    ): Response<Map<String, String>>

    @DELETE("api/v1/notifications")
    suspend fun deleteAllNotifications(): Response<Map<String, String>>
}
