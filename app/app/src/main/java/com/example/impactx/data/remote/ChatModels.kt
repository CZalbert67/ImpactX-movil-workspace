package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

// ============ QUICK MESSAGES / CHAT DTOs ============

data class UpsertQuickMessageTemplateRequest(
    @SerializedName("text") val text: String,
    @SerializedName("sortOrder") val sortOrder: Int = 0
)

data class SendQuickMessageRequest(
    @SerializedName("recipientPublicProfileId") val recipientPublicProfileId: String,
    @SerializedName("publicTemplateId") val publicTemplateId: String,
    @SerializedName("routePublicId") val routePublicId: String? = null,
    @SerializedName("incidentPublicId") val incidentPublicId: String? = null
)

data class QuickMessageTemplateDto(
    @SerializedName("publicTemplateId") val publicTemplateId: String,
    @SerializedName("text") val text: String,
    @SerializedName("sortOrder") val sortOrder: Int,
    @SerializedName("isSystem") val isSystem: Boolean
)

data class QuickMessageDto(
    @SerializedName("publicMessageId") val publicMessageId: String,
    @SerializedName("senderPublicProfileId") val senderPublicProfileId: String,
    @SerializedName("senderUsername") val senderUsername: String,
    @SerializedName("recipientPublicProfileId") val recipientPublicProfileId: String,
    @SerializedName("recipientUsername") val recipientUsername: String,
    @SerializedName("publicRelationshipId") val publicRelationshipId: String,
    @SerializedName("publicTemplateId") val publicTemplateId: String,
    @SerializedName("text") val text: String,
    @SerializedName("routePublicId") val routePublicId: String?,
    @SerializedName("incidentPublicId") val incidentPublicId: String?,
    @SerializedName("sentAtUtc") val sentAtUtc: String,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("readAtUtc") val readAtUtc: String?
)
