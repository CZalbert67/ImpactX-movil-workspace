package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

// ============ QUICK MESSAGES / CHAT ============
data class SendQuickMessageRequest(
    @SerializedName("recipientPublicProfileId") val recipientPublicProfileId: String,
    @SerializedName("body") val body: String,
    @SerializedName("templateId") val templateId: String? = null
)

data class QuickMessageDto(
    @SerializedName("publicMessageId") val publicMessageId: String,
    @SerializedName("senderId") val senderId: String,
    @SerializedName("recipientId") val recipientId: String,
    @SerializedName("body") val body: String,
    @SerializedName("sentAt") val sentAt: String,
    @SerializedName("isRead") val isRead: Boolean
)

data class QuickMessageTemplateDto(
    @SerializedName("publicTemplateId") val publicTemplateId: String,
    @SerializedName("body") val body: String
)
