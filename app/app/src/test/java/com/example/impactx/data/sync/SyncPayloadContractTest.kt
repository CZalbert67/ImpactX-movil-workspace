package com.example.impactx.data.sync

import com.example.impactx.data.remote.SosRequest
import com.example.impactx.data.remote.TelemetryBatchRequestV2
import com.example.impactx.data.remote.TelemetryEventRequestV2
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPayloadContractTest {
    private val gson = Gson()

    @Test
    fun sosIncludesWearableEventIdForIdempotency() {
        val json = gson.toJson(
            SosRequest(
                lat = 19.9,
                lng = -99.3,
                severidad = "severe",
                canal = "wearable-relay-mobile",
                gForce = "12.50",
                frecuenciaCardiaca = "88",
                modo = "immediate",
                viajeId = "trip-1",
                clientEventId = "73de95c8-8111-4c88-a4cc-1e03643da47f",
                capturedOffline = true,
                occurredAtUtc = "2026-08-06T15:00:00Z",
            ),
        )
        assertTrue(json.contains("clientEventId"))
        assertTrue(json.contains("immediate"))
        assertTrue(json.contains("capturedOffline"))
        assertTrue(json.contains("occurredAtUtc"))
    }

    @Test
    fun telemetryUsesSchemaV2AndEventos() {
        val request = TelemetryBatchRequestV2(
            batchId = "3ecfd8c0-2b48-41b8-92af-aebcf86dc7c9",
            batchSequence = 1,
            capturedOffline = true,
            wearableDeviceId = "GW8-001",
            batteryLevel = 80,
            eventos = listOf(
                TelemetryEventRequestV2(
                    eventId = "9dcd1b9f-1aa8-4344-a4e0-e70129dcc5e6",
                    timestamp = "2026-08-06T15:00:00Z",
                    sequenceNumber = 1,
                    lat = 19.9,
                    lng = -99.3,
                    velocidad = 12.0,
                    gpsAccuracyMeters = 8.0,
                    aceleracionX = 0.1,
                    aceleracionY = 0.2,
                    aceleracionZ = 9.8,
                    magnitudAceleracion = 9.81,
                    giroscopioX = 0.0,
                    giroscopioY = 0.0,
                    giroscopioZ = 0.0,
                    frecuenciaCardiaca = 80,
                ),
            ),
        )
        val json = gson.toJsonTree(request).asJsonObject
        assertEquals(2, json["schemaVersion"].asInt)
        assertTrue(json["capturedOffline"].asBoolean)
        assertEquals(80, json["batteryLevel"].asInt)
        assertEquals(1, json["eventos"].asJsonArray.size())
        assertEquals(8.0, json["eventos"].asJsonArray[0].asJsonObject["gpsAccuracyMeters"].asDouble, 0.0)
    }
}
