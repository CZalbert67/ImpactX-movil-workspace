package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

// ============ TRIPS ============
data class StartTripRequest(
    @SerializedName("dispositivoId") val dispositivoId: String,
    @SerializedName("proposito") val proposito: String? = null,
    @SerializedName("rutaOrigen") val rutaOrigen: String? = null,
    @SerializedName("rutaDestino") val rutaDestino: String? = null,
    @SerializedName("vehiclePublicId") val vehiclePublicId: String? = null
)

data class ViajeDto(
    @SerializedName("id") val id: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("inicio") val inicio: String,
    @SerializedName("fin") val fin: String?,
    @SerializedName("rutaOrigen") val rutaOrigen: String?,
    @SerializedName("distanciaRecorridaKm") val distanciaRecorridaKm: Double?
)

data class TripActionResponse(
    @SerializedName("viajeId") val viajeId: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("mensaje") val mensaje: String
)
