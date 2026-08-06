package com.example.impactx.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TelemetryQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIfAbsent(TelemetryQueueEntity event);

    @Query("SELECT COUNT(*) FROM telemetry_queue WHERE status IN ('PENDING','FAILED','SENDING')")
    int countPending();

    @Query("SELECT MIN(createdAtMs) FROM telemetry_queue WHERE status IN ('PENDING','FAILED','SENDING')")
    Long oldestPendingCreatedAtMs();

    @Query("SELECT tripId FROM telemetry_queue WHERE status IN ('PENDING','FAILED','SENDING') ORDER BY createdAtMs ASC LIMIT 1")
    String oldestPendingTripId();

    @Query("SELECT * FROM telemetry_queue WHERE tripId=:tripId AND status IN ('PENDING','FAILED','SENDING') ORDER BY sequenceNumber ASC, createdAtMs ASC LIMIT :limit")
    List<TelemetryQueueEntity> getPendingForTrip(String tripId, int limit);

    @Query("UPDATE telemetry_queue SET status='SENDING', batchId=:batchId, lastError=NULL WHERE eventId IN (:eventIds)")
    void markBatchSending(List<String> eventIds, String batchId);

    @Query("UPDATE telemetry_queue SET status='SENT', batchId=:batchId, sentAtMs=:sentAtMs, lastError=NULL WHERE eventId IN (:eventIds)")
    void markBatchSent(List<String> eventIds, String batchId, long sentAtMs);

    @Query("UPDATE telemetry_queue SET status='FAILED', batchId=:batchId, lastError=:error WHERE eventId IN (:eventIds)")
    void markBatchFailed(List<String> eventIds, String batchId, String error);

    @Query("UPDATE telemetry_queue SET status='PENDING', lastError=NULL WHERE batchId=:batchId")
    int requeueBatch(String batchId);

    @Query("SELECT * FROM telemetry_queue WHERE batchId=:batchId ORDER BY sequenceNumber ASC, createdAtMs ASC")
    List<TelemetryQueueEntity> getByBatchId(String batchId);

    @Query("SELECT * FROM telemetry_queue ORDER BY createdAtMs DESC LIMIT :limit")
    List<TelemetryQueueEntity> getRecent(int limit);
}
