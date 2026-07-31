package com.example.impactx.data.local

import androidx.room.*

@Dao
interface SessionDao {
    @Query("SELECT * FROM user_session LIMIT 1")
    suspend fun getSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    @Query("DELETE FROM user_session")
    suspend fun clearSession()
}
