package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

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
