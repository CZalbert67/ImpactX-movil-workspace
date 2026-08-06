package com.example.impactx.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Durable SOS queue. The wearable-generated eventId is the idempotency key. */
@Entity(tableName = "pending_sos")
public class PendingSosEntity {
    @PrimaryKey
    @NonNull
    public String eventId = "";

    public String sourceNodeId;
    public String tripId;
    public int localAccidentId;
    public double lat;
    public double lng;
    public String place;
    public String severity;
    public String channel;
    public String gForce;
    public String heartRate;
    public String mode;
    public String timestampUtc;
    public boolean capturedOffline;
    public String status;
    public int attempts;
    public String lastError;
    public String backendAlertId;
    public long createdAtMs;
    public long sentAtMs;
}
