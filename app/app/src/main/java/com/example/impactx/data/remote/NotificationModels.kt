package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

data class NotificacionDto(
    @SerializedName("id") val id: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("referenciaId") val referenciaId: String?,
    @SerializedName("referenciaTipo") val referenciaTipo: String?,
    @SerializedName("publicRelationshipId") val publicRelationshipId: String?,
    @SerializedName("leida") val leida: Boolean,
    @SerializedName("leidaEn") val leidaEn: String?,
    @SerializedName("creadoEn") val creadoEn: String
)

data class ToggleReadRequest(
    @SerializedName("leida") val leida: Boolean
)
