package com.example.impactx.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AccidentDao {
    @Insert
    void insertAccident(AccidentEntity accident);

    @Query("SELECT * FROM accident_records WHERE sent = 0 ORDER BY id DESC")
    List<AccidentEntity> getPendingAccidents();

    @Query("SELECT * FROM accident_records ORDER BY id DESC")
    List<AccidentEntity> getAllAccidents();

    @Update
    void updateAccident(AccidentEntity accident);

    @Query("UPDATE accident_records SET sent = 1 WHERE id = :id")
    void markAsSent(int id);
}
