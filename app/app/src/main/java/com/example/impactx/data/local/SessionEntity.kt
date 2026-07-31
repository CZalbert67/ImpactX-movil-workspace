package com.example.impactx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class SessionEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val correo: String,
    val planActivo: String?,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long // Timestamp in milliseconds when access token expires
)
