package com.example.impactx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accident_records")
data class AccidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val heartRate: Int,
    val gForce: Double,
    val timestamp: String,
    val lat: Double,
    val lng: Double,
    val sent: Boolean = false
)
