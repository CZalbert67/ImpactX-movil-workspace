package com.example.impactx.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "user_session")
public class SessionEntity {
    @PrimaryKey
    @NonNull
    public String userId;
    
    public String username;
    public String correo;
    public String planActivo;
    public String accessToken;
    public String refreshToken;
    public long expiresAt;

    @Ignore
    public SessionEntity() {
        this.userId = "";
    }

    public SessionEntity(@NonNull String userId, String username, String correo, String planActivo, String accessToken, String refreshToken, long expiresAt) {
        this.userId = userId;
        this.username = username;
        this.correo = correo;
        this.planActivo = planActivo;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}
