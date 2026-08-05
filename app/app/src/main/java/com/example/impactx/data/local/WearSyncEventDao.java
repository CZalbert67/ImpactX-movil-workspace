package com.example.impactx.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface WearSyncEventDao {

    /**
     * Try to insert a new event.
     * IGNORE on conflict means: if the same eventId already exists, nothing happens.
     * Returns the new rowId, or -1 if ignored (duplicate).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertEventIfAbsent(WearSyncEventEntity event);

    @Query("SELECT * FROM wear_sync_events WHERE eventId = :eventId LIMIT 1")
    WearSyncEventEntity findByEventId(String eventId);

    @Query("UPDATE wear_sync_events SET status = :status, httpCode = :httpCode, backendEntityId = :backendEntityId, updatedAtUtc = :updatedAt WHERE eventId = :eventId")
    void updateStatus(String eventId, String status, int httpCode, String backendEntityId, String updatedAt);

    @Query("UPDATE wear_sync_events SET status = :status, httpCode = :httpCode, errorMessage = :errorMessage, updatedAtUtc = :updatedAt WHERE eventId = :eventId")
    void updateFailure(String eventId, String status, int httpCode, String errorMessage, String updatedAt);

    /** Returns true if the event was already successfully processed. */
    @Query("SELECT COUNT(*) FROM wear_sync_events WHERE eventId = :eventId AND status = 'SUCCEEDED'")
    int isSucceeded(String eventId);
}
