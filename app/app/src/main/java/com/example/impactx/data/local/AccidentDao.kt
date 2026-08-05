package com.example.impactx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AccidentDao {
    @Insert
    suspend fun insertAccident(accident: AccidentEntity)

    @Query("SELECT * FROM accident_records WHERE sent = 0 ORDER BY id DESC")
    suspend fun getPendingAccidents(): List<AccidentEntity>

    @Query("SELECT * FROM accident_records ORDER BY id DESC")
    suspend fun getAllAccidents(): List<AccidentEntity>

    @Update
    suspend fun updateAccident(accident: AccidentEntity)

    @Query("UPDATE accident_records SET sent = 1 WHERE id = :id")
    suspend fun markAsSent(id: Int)
}
