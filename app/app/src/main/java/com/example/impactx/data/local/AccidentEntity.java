package com.example.impactx.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accident_records")
public class AccidentEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int heartRate;
    private double gForce;
    private String timestamp;
    private double lat;
    private double lng;
    private boolean sent;

    public AccidentEntity() {}

    public AccidentEntity(int heartRate, double gForce, String timestamp, double lat, double lng, boolean sent) {
        this.heartRate = heartRate;
        this.gForce = gForce;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lng = lng;
        this.sent = sent;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getHeartRate() { return heartRate; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }

    public double getGForce() { return gForce; }
    public void setGForce(double gForce) { this.gForce = gForce; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public boolean isSent() { return sent; }
    public boolean getSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
}
