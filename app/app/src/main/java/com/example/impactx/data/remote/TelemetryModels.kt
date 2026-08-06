package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

data class TelemetryBatchRequestV2(
    @SerializedName("schemaVersion") val schemaVersion: Int = 2,
    @SerializedName("batchId") val batchId: String,
    @SerializedName("batchSequence") val batchSequence: Long,
    @SerializedName("capturedOffline") val capturedOffline: Boolean,
    @SerializedName("wearableDeviceId") val wearableDeviceId: String,
    @SerializedName("wearableModel") val wearableModel: String = "Galaxy Watch 8",
    @SerializedName("wearableAppVersion") val wearableAppVersion: String? = null,
    @SerializedName("wearableOsVersion") val wearableOsVersion: String? = null,
    @SerializedName("wearableFirmwareVersion") val wearableFirmwareVersion: String? = null,
    @SerializedName("batteryLevel") val batteryLevel: Int? = null,
    @SerializedName("eventos") val eventos: List<TelemetryEventRequestV2>,
)

data class TelemetryEventRequestV2(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("sequenceNumber") val sequenceNumber: Long?,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("velocidad") val velocidad: Double,
    @SerializedName("gpsAccuracyMeters") val gpsAccuracyMeters: Double,
    @SerializedName("aceleracionX") val aceleracionX: Double?,
    @SerializedName("aceleracionY") val aceleracionY: Double?,
    @SerializedName("aceleracionZ") val aceleracionZ: Double?,
    @SerializedName("magnitudAceleracion") val magnitudAceleracion: Double?,
    @SerializedName("giroscopioX") val giroscopioX: Double?,
    @SerializedName("giroscopioY") val giroscopioY: Double?,
    @SerializedName("giroscopioZ") val giroscopioZ: Double?,
    @SerializedName("frecuenciaCardiaca") val frecuenciaCardiaca: Int?,
    @SerializedName("calidadSensor") val calidadSensor: String = "high",
    @SerializedName("sensorFlags") val sensorFlags: List<String> = emptyList(),
)

data class TelemetryIngestionResultDto(
    @SerializedName("viajeId") val viajeId: String,
    @SerializedName("batchId") val batchId: String?,
    @SerializedName("schemaVersion") val schemaVersion: Int,
    @SerializedName("capturedOffline") val capturedOffline: Boolean,
    @SerializedName("recibidos") val recibidos: Int,
    @SerializedName("insertados") val insertados: Int,
    @SerializedName("duplicados") val duplicados: Int,
    @SerializedName("procesadoEnUtc") val procesadoEnUtc: String,
)
