package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

// ---- Monitoring Relationships ----
data class MonitoringPermissionsDto(
    @SerializedName("viewRoutes") val viewRoutes: Boolean = false,
    @SerializedName("viewLocation") val viewLocation: Boolean = false,
    @SerializedName("viewEmergencyLocation") val viewEmergencyLocation: Boolean = false,
    @SerializedName("viewIncidents") val viewIncidents: Boolean = false,
    @SerializedName("receiveCriticalAlerts") val receiveCriticalAlerts: Boolean = false,
    @SerializedName("viewMedicalProfile") val viewMedicalProfile: Boolean = false,
    @SerializedName("sendMessages") val sendMessages: Boolean = false,
    @SerializedName("viewTelemetry") val viewTelemetry: Boolean = false,
    @SerializedName("receiveNotifications") val receiveNotifications: Boolean = false
)

data class MonitoringPermissionsRequest(
    @SerializedName("viewRoutes") val viewRoutes: Boolean = true,
    @SerializedName("viewLocation") val viewLocation: Boolean = true,
    @SerializedName("viewEmergencyLocation") val viewEmergencyLocation: Boolean = true,
    @SerializedName("viewIncidents") val viewIncidents: Boolean = true,
    @SerializedName("receiveCriticalAlerts") val receiveCriticalAlerts: Boolean = true,
    @SerializedName("sendMessages") val sendMessages: Boolean = true,
    @SerializedName("viewTelemetry") val viewTelemetry: Boolean = true,
    @SerializedName("receiveNotifications") val receiveNotifications: Boolean = true
)

data class MonitoringRelationshipDto(
    @SerializedName("publicRelationshipId") val publicRelationshipId: String,
    @SerializedName("status") val status: String, // Pending, Accepted, Rejected, Revoked, Blocked, Expired
    @SerializedName("direction") val direction: String, // MonitorInvitesMonitored, MonitoredRequestsMonitor
    @SerializedName("monitorPublicProfileId") val monitorPublicProfileId: String,
    @SerializedName("monitorUsername") val monitorUsername: String,
    @SerializedName("monitorName") val monitorName: String,
    @SerializedName("monitoredPublicProfileId") val monitoredPublicProfileId: String?,
    @SerializedName("monitoredUsername") val monitoredUsername: String?,
    @SerializedName("monitoredName") val monitoredName: String?,
    @SerializedName("permissions") val permissions: MonitoringPermissionsDto = MonitoringPermissionsDto(),
    @SerializedName("requestedAtUtc") val requestedAtUtc: String,
    @SerializedName("expiresAtUtc") val expiresAtUtc: String
)

data class CreateMonitoringInvitationRequest(
    @SerializedName("username") val username: String? = null,
    @SerializedName("publicProfileId") val publicProfileId: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("permissions") val permissions: MonitoringPermissionsRequest? = MonitoringPermissionsRequest()
)

data class CreateMonitoringInvitationResponse(
    @SerializedName("relationship") val relationship: MonitoringRelationshipDto,
    @SerializedName("manualCode") val manualCode: String
)

data class AcceptMonitoringInvitationRequest(
    @SerializedName("publicRelationshipId") val publicRelationshipId: String? = null,
    @SerializedName("code") val code: String? = null
)

data class RespondMonitoringInvitationRequest(
    @SerializedName("publicRelationshipId") val publicRelationshipId: String? = null,
    @SerializedName("code") val code: String? = null
)

data class UpdateMonitoringPermissionsRequest(
    @SerializedName("viewRoutes") val viewRoutes: Boolean,
    @SerializedName("viewLocation") val viewLocation: Boolean,
    @SerializedName("viewEmergencyLocation") val viewEmergencyLocation: Boolean,
    @SerializedName("viewIncidents") val viewIncidents: Boolean,
    @SerializedName("receiveCriticalAlerts") val receiveCriticalAlerts: Boolean,
    @SerializedName("viewMedicalProfile") val viewMedicalProfile: Boolean,
    @SerializedName("sendMessages") val sendMessages: Boolean,
    @SerializedName("viewTelemetry") val viewTelemetry: Boolean,
    @SerializedName("receiveNotifications") val receiveNotifications: Boolean
)

data class UserProfileUsernameDto(
    @SerializedName("publicProfileId") val publicProfileId: String,
    @SerializedName("username") val username: String
)

data class MonitorDto(
    @SerializedName("id") val id: String,
    @SerializedName("correoInvitado") val correoInvitado: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("permisos") val permisos: List<String>
)

data class InviteMonitorRequest(
    @SerializedName("correoInvitado") val correoInvitado: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("permisos") val permisos: List<String> = listOf("ViewLocation", "ReceiveSOS")
)
