package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

// ============ FAMILY SUBSCRIPTIONS V2 DTOs ============

enum class FamilySubscriptionStatus {
    @SerializedName("Active") Active,
    @SerializedName("PastDue") PastDue,
    @SerializedName("Suspended") Suspended,
    @SerializedName("Cancelled") Cancelled,
    @SerializedName("Expired") Expired
}

enum class FamilyMembershipRole {
    @SerializedName("Owner") Owner,
    @SerializedName("Member") Member
}

enum class FamilyMembershipStatus {
    @SerializedName("Pending") Pending,
    @SerializedName("Active") Active,
    @SerializedName("Rejected") Rejected,
    @SerializedName("Left") Left,
    @SerializedName("Removed") Removed,
    @SerializedName("Expired") Expired
}

enum class FamilyInvitationStatus {
    @SerializedName("Pending") Pending,
    @SerializedName("Accepted") Accepted,
    @SerializedName("Rejected") Rejected,
    @SerializedName("Expired") Expired,
    @SerializedName("Revoked") Revoked,
    @SerializedName("Consumed") Consumed
}

data class ActivateFamilySubscriptionRequest(
    @SerializedName("planName") val planName: String
)

data class ChangeFamilyPlanRequest(
    @SerializedName("planName") val planName: String
)

data class CreateFamilyInvitationRequest(
    @SerializedName("username") val username: String? = null,
    @SerializedName("publicProfileId") val publicProfileId: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("createMonitoringRelationship") val createMonitoringRelationship: Boolean = true
)

data class RedeemFamilyInvitationRequest(
    @SerializedName("code") val code: String
)

data class SimulatedPaymentDto(
    @SerializedName("publicPaymentId") val publicPaymentId: String,
    @SerializedName("result") val result: String,
    @SerializedName("planName") val planName: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("occurredAtUtc") val occurredAtUtc: String
)

data class FamilySubscriptionSummaryDto(
    @SerializedName("publicSubscriptionId") val publicSubscriptionId: String,
    @SerializedName("planName") val planName: String,
    @SerializedName("status") val status: FamilySubscriptionStatus,
    @SerializedName("currentUserRole") val currentUserRole: FamilyMembershipRole,
    @SerializedName("ownerPublicProfileId") val ownerPublicProfileId: String,
    @SerializedName("ownerUsername") val ownerUsername: String,
    @SerializedName("ownerName") val ownerName: String,
    @SerializedName("acceptedMembers") val acceptedMembers: Int,
    @SerializedName("invitedMemberLimit") val invitedMemberLimit: Int,
    @SerializedName("totalActivePeople") val totalActivePeople: Int,
    @SerializedName("totalPeopleLimit") val totalPeopleLimit: Int,
    @SerializedName("pendingInvitationCount") val pendingInvitationCount: Int,
    @SerializedName("availableMemberSlots") val availableMemberSlots: Int,
    @SerializedName("vehicleLimitPerUser") val vehicleLimitPerUser: Int,
    @SerializedName("pendingAdjustment") val pendingAdjustment: Boolean,
    @SerializedName("pendingPlanName") val pendingPlanName: String?,
    @SerializedName("periodStartUtc") val periodStartUtc: String,
    @SerializedName("periodEndUtc") val periodEndUtc: String,
    @SerializedName("nextBillingAtUtc") val nextBillingAtUtc: String?,
    @SerializedName("graceEndsAtUtc") val graceEndsAtUtc: String?,
    @SerializedName("autoRenew") val autoRenew: Boolean,
    @SerializedName("canManagePlan") val canManagePlan: Boolean,
    @SerializedName("canInviteMembers") val canInviteMembers: Boolean,
    @SerializedName("canLeaveGroup") val canLeaveGroup: Boolean,
    @SerializedName("sosContactLimit") val sosContactLimit: Int,
    @SerializedName("latestPayment") val latestPayment: SimulatedPaymentDto?
)

data class FamilyMemberDto(
    @SerializedName("publicMembershipId") val publicMembershipId: String,
    @SerializedName("publicProfileId") val publicProfileId: String,
    @SerializedName("username") val username: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("role") val role: FamilyMembershipRole,
    @SerializedName("status") val status: FamilyMembershipStatus,
    @SerializedName("acceptedAtUtc") val acceptedAtUtc: String?
)

data class FamilyInvitationDto(
    @SerializedName("publicInvitationId") val publicInvitationId: String,
    @SerializedName("targetUsername") val targetUsername: String?,
    @SerializedName("targetPublicProfileId") val targetPublicProfileId: String?,
    @SerializedName("targetEmail") val targetEmail: String?,
    @SerializedName("status") val status: FamilyInvitationStatus,
    @SerializedName("createdAtUtc") val createdAtUtc: String,
    @SerializedName("expiresAtUtc") val expiresAtUtc: String
)

data class IncomingFamilyInvitationDto(
    @SerializedName("publicInvitationId") val publicInvitationId: String,
    @SerializedName("targetUsername") val targetUsername: String?,
    @SerializedName("targetPublicProfileId") val targetPublicProfileId: String?,
    @SerializedName("targetEmail") val targetEmail: String?,
    @SerializedName("status") val status: FamilyInvitationStatus,
    @SerializedName("createdAtUtc") val createdAtUtc: String,
    @SerializedName("expiresAtUtc") val expiresAtUtc: String,
    @SerializedName("ownerPublicProfileId") val ownerPublicProfileId: String,
    @SerializedName("ownerUsername") val ownerUsername: String,
    @SerializedName("ownerName") val ownerName: String,
    @SerializedName("planName") val planName: String
)

data class CreateFamilyInvitationResponse(
    @SerializedName("invitation") val invitation: FamilyInvitationDto,
    @SerializedName("manualCode") val manualCode: String
)


data class FamilyMemberAccessDto(
    @SerializedName("publicRelationshipId") val publicRelationshipId: String,
    @SerializedName("publicSubscriptionId") val publicSubscriptionId: String,
    @SerializedName("subjectPublicProfileId") val subjectPublicProfileId: String,
    @SerializedName("subjectUsername") val subjectUsername: String,
    @SerializedName("subjectName") val subjectName: String,
    @SerializedName("viewerPublicProfileId") val viewerPublicProfileId: String,
    @SerializedName("viewerUsername") val viewerUsername: String,
    @SerializedName("viewerName") val viewerName: String,
    @SerializedName("permissions") val permissions: MonitoringPermissionsDto,
    @SerializedName("medicalConsentGranted") val medicalConsentGranted: Boolean,
    @SerializedName("sosPriority") val sosPriority: Int?,
    @SerializedName("isSosContact") val isSosContact: Boolean,
    @SerializedName("updatedAtUtc") val updatedAtUtc: String
)

data class UpdateFamilyMemberAccessRequest(
    @SerializedName("viewRoutes") val viewRoutes: Boolean,
    @SerializedName("viewLocation") val viewLocation: Boolean,
    @SerializedName("viewEmergencyLocation") val viewEmergencyLocation: Boolean = true,
    @SerializedName("viewIncidents") val viewIncidents: Boolean = true,
    @SerializedName("receiveCriticalAlerts") val receiveCriticalAlerts: Boolean = true,
    @SerializedName("viewMedicalProfile") val viewMedicalProfile: Boolean,
    @SerializedName("sendMessages") val sendMessages: Boolean = true,
    @SerializedName("viewTelemetry") val viewTelemetry: Boolean,
    @SerializedName("receiveNotifications") val receiveNotifications: Boolean = true,
    @SerializedName("confirmMedicalConsent") val confirmMedicalConsent: Boolean,
    @SerializedName("sosPriority") val sosPriority: Int?
)
