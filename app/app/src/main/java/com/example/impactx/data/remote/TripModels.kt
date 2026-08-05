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

// ============ WEARABLE ============
data class PairWearableRequest(
    @SerializedName("dispositivoId") val dispositivoId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("modelo") val modelo: String,
    @SerializedName("fabricante") val fabricante: String = "Samsung",
    @SerializedName("plataforma") val plataforma: String = "WearOS",
    @SerializedName("versionSistemaOperativo") val versionSistemaOperativo: String? = null,
    @SerializedName("versionFirmware") val versionFirmware: String? = null,
    @SerializedName("appVersion") val appVersion: String? = null,
    @SerializedName("capacidadesSensores") val capacidadesSensores: List<String> = emptyList()
)

data class PairConfirmRequest(
    @SerializedName("token") val token: String
)

data class PairResponse(
    @SerializedName("token") val token: String,
    @SerializedName("expiresAtUtc") val expiresAtUtc: String,
    @SerializedName("mensaje") val mensaje: String
)

data class WearableDto(
    @SerializedName("id") val id: String,
    @SerializedName("dispositivoId") val dispositivoId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("modelo") val modelo: String,
    @SerializedName("fabricante") val fabricante: String,
    @SerializedName("plataforma") val plataforma: String,
    @SerializedName("estado") val estado: String
)

// ============ TELEMETRY ============
data class TelemetryPointDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("velocidad") val velocidad: Double = 0.0,
    @SerializedName("timestamp") val timestamp: String
)

data class TelemetryBatchRequest(
    @SerializedName("puntos") val puntos: List<TelemetryPointDto>
)

