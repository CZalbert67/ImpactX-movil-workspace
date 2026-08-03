package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

// ============ ALERTS / SOS ============
data class SosRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("lugar") val lugar: String? = null,
    @SerializedName("severidad") val severidad: String = "severe",
    @SerializedName("canal") val canal: String = "wearable",
    @SerializedName("gForce") val gForce: String? = null,
    @SerializedName("frecuenciaCardiaca") val frecuenciaCardiaca: String? = null,
    @SerializedName("modo") val modo: String = "automatico",
    @SerializedName("viajeId") val viajeId: String? = null
)

data class DetectAlertRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("lugar") val lugar: String? = null,
    @SerializedName("gForce") val gForce: Double,
    @SerializedName("decibeles") val decibeles: Double = 0.0,
    @SerializedName("frecuenciaCardiaca") val frecuenciaCardiaca: Double,
    @SerializedName("severidad") val severidad: String = "crash",
    @SerializedName("viajeId") val viajeId: String? = null
)

data class AlertStatusDto(
    @SerializedName("id") val id: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("severidad") val severidad: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("lugar") val lugar: String?,
    @SerializedName("creadoEn") val creadoEn: String,
    @SerializedName("contactosNotificados") val contactosNotificados: List<String>
)
