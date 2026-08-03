package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

// ============ EMERGENCY CONTACT RELATIONSHIPS DTOs ============

data class CreateEmergencyContactInvitationRequest(
    @SerializedName("username") val username: String?,
    @SerializedName("publicProfileId") val publicProfileId: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("relationship") val relationship: String?,
    @SerializedName("priority") val priority: String = "Secondary",
    @SerializedName("makePrimaryWhenAccepted") val makePrimaryWhenAccepted: Boolean = false
)

data class RespondEmergencyContactInvitationRequest(
    @SerializedName("publicContactId") val publicContactId: String? = null,
    @SerializedName("code") val code: String?
)

data class UpdateEmergencyContactRequest(
    @SerializedName("relationship") val relationship: String?,
    @SerializedName("priority") val priority: String?
)

data class EmergencyContactDto(
    @SerializedName("publicContactId") val publicContactId: String,
    @SerializedName("status") val status: String,
    @SerializedName("isOwner") val isOwner: Boolean,
    @SerializedName("ownerPublicProfileId") val ownerPublicProfileId: String,
    @SerializedName("ownerUsername") val ownerUsername: String,
    @SerializedName("ownerName") val ownerName: String,
    @SerializedName("contactPublicProfileId") val contactPublicProfileId: String?,
    @SerializedName("contactUsername") val contactUsername: String?,
    @SerializedName("contactName") val contactName: String?,
    @SerializedName("targetEmailHint") val targetEmailHint: String?,
    @SerializedName("relationship") val relationship: String?,
    @SerializedName("priority") val priority: String,
    @SerializedName("isPrimary") val isPrimary: Boolean,
    @SerializedName("requestedAtUtc") val requestedAtUtc: String,
    @SerializedName("expiresAtUtc") val expiresAtUtc: String,
    @SerializedName("acceptedAtUtc") val acceptedAtUtc: String?,
    @SerializedName("rejectedAtUtc") val rejectedAtUtc: String?,
    @SerializedName("revokedAtUtc") val revokedAtUtc: String?,
    @SerializedName("blockedAtUtc") val blockedAtUtc: String?,
    @SerializedName("updatedAtUtc") val updatedAtUtc: String
)

data class CreateEmergencyContactInvitationResponse(
    @SerializedName("contact") val contact: EmergencyContactDto,
    @SerializedName("manualCode") val manualCode: String
)

// Legacy compatibility DTOs if referenced elsewhere (deprecating)
data class ContactoDto(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("parentesco") val parentesco: String?,
    @SerializedName("priority") val priority: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean
)
