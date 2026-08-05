package com.example.impactx.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Stores the association between a Wear OS node and a backend device registration.
 *
 * Identity chain:
 *   sourceNodeId (Data Layer, may change) →
 *   installationId (stable UUID from wearable SharedPreferences) →
 *   backendDeviceId (dispositivoId registered in the backend)
 *
 * When a wearable reconnects with a new sourceNodeId (e.g. after a reboot),
 * the installationId allows us to find the existing linkage and update only
 * the sourceNodeId, without triggering a new backend pairing.
 */
@Entity(tableName = "wearable_linkage")
public class WearableLinkageEntity {
    @PrimaryKey
    @NonNull
    public String nodeId;

    /** Stable UUID generated once by the wearable and never changed. */
    @ColumnInfo(name = "installation_id")
    public String installationId;

    public String backendDeviceId;
    public String nombre;
    public String modelo;
    public String fabricante;
    public String estado;
    public long linkedAt;

    /** Epoch ms when the mobile last received a /device-info or /telemetry from this node. */
    @ColumnInfo(name = "last_seen_at_ms")
    public long lastSeenAtMs;

    public WearableLinkageEntity() {
        this.nodeId = "";
        this.installationId = "";
        this.lastSeenAtMs = 0;
    }

    @Ignore
    public WearableLinkageEntity(
            @NonNull String nodeId,
            String installationId,
            String backendDeviceId,
            String nombre,
            String modelo,
            String fabricante,
            String estado,
            long linkedAt) {
        this.nodeId = nodeId;
        this.installationId = installationId != null ? installationId : "";
        this.backendDeviceId = backendDeviceId;
        this.nombre = nombre;
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.estado = estado;
        this.linkedAt = linkedAt;
        this.lastSeenAtMs = System.currentTimeMillis();
    }

    /**
     * Legacy constructor for backward compatibility.
     * installationId defaults to empty string (will be updated on next /device-info).
     */
    @Ignore
    public WearableLinkageEntity(
            @NonNull String nodeId,
            String backendDeviceId,
            String nombre,
            String modelo,
            String fabricante,
            String estado,
            long linkedAt) {
        this(nodeId, "", backendDeviceId, nombre, modelo, fabricante, estado, linkedAt);
    }
}
