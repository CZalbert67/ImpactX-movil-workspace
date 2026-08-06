package com.example.impactx.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PendingSosDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIfAbsent(PendingSosEntity event);

    @Query("SELECT * FROM pending_sos WHERE status IN ('PENDING','FAILED','SENDING') ORDER BY createdAtMs ASC LIMIT :limit")
    List<PendingSosEntity> getPending(int limit);

    @Query("SELECT COUNT(*) FROM pending_sos WHERE status IN ('PENDING','FAILED','SENDING')")
    int countPending();

    @Query("SELECT * FROM pending_sos ORDER BY createdAtMs DESC LIMIT :limit")
    List<PendingSosEntity> getRecent(int limit);

    @Query("UPDATE pending_sos SET status='SENDING', attempts=attempts+1, lastError=NULL WHERE eventId=:eventId")
    void markSending(String eventId);

    @Query("UPDATE pending_sos SET status='SENT', backendAlertId=:backendAlertId, sentAtMs=:sentAtMs, lastError=NULL WHERE eventId=:eventId")
    void markSent(String eventId, String backendAlertId, long sentAtMs);

    @Query("UPDATE pending_sos SET status='FAILED', lastError=:error WHERE eventId=:eventId")
    void markFailed(String eventId, String error);
}
