package com.example.impactx.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SessionDao {
    @Query("SELECT * FROM user_session LIMIT 1")
    SessionEntity getSession();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveSession(SessionEntity session);

    @Query("DELETE FROM user_session")
    void clearSession();
}
