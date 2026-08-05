package com.example.impactx.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "wearable_linkage")
public class WearableLinkageEntity {
    @PrimaryKey
    @NonNull
    public String nodeId;
    
    public String backendDeviceId;
    public String nombre;
    public String modelo;
    public String fabricante;
    public String estado;
    public long linkedAt;

    public WearableLinkageEntity() {
        this.nodeId = "";
    }

    @Ignore
    public WearableLinkageEntity(@NonNull String nodeId, String backendDeviceId, String nombre, String modelo, String fabricante, String estado, long linkedAt) {
        this.nodeId = nodeId;
        this.backendDeviceId = backendDeviceId;
        this.nombre = nombre;
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.estado = estado;
        this.linkedAt = linkedAt;
    }
}
