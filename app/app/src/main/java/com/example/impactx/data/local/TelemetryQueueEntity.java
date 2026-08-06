package com.example.impactx.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Telemetry captured by the Galaxy Watch8 and relayed by the phone. */
@Entity(
    tableName = "telemetry_queue",
    indices = {
        @Index(name = "index_telemetry_queue_trip_status", value = {"tripId", "status"}),
        @Index(name = "index_telemetry_queue_batchId", value = {"batchId"})
    }
)
public class TelemetryQueueEntity {
    @PrimaryKey
    @NonNull
    public String eventId = "";

    @NonNull
    public String tripId = "";

    public Long sequenceNumber;
    public String timestampUtc;
    public double lat;
    public double lng;
    public double velocity;
    public Double gpsAccuracyMeters;
    public Double accelerationX;
    public Double accelerationY;
    public Double accelerationZ;
    public Double accelerationMagnitude;
    public Double gyroscopeX;
    public Double gyroscopeY;
    public Double gyroscopeZ;
    public Integer heartRate;
    public Integer batteryLevel;
    public boolean capturedOffline;
    public String wearableDeviceId;
    public String wearableModel;
    public String status;
    public String batchId;
    public String lastError;
    public long createdAtMs;
    public long sentAtMs;
}
