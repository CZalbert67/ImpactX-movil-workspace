package com.example.impactx.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface WearableLinkageDao {

    /** Find linkage by the Data Layer sourceNodeId (may change after reboot). */
    @Query("SELECT * FROM wearable_linkage WHERE nodeId = :nodeId LIMIT 1")
    WearableLinkageEntity getLinkageByNodeId(String nodeId);

    /**
     * Find linkage by the stable installationId UUID from the wearable.
     * Use this when the sourceNodeId has changed (reboot, re-pair) but the
     * device is the same physical device.
     */
    @Query("SELECT * FROM wearable_linkage WHERE installation_id = :installationId AND installation_id != '' LIMIT 1")
    WearableLinkageEntity getLinkageByInstallationId(String installationId);

    /** Get the first (and usually only) linked wearable. */
    @Query("SELECT * FROM wearable_linkage LIMIT 1")
    WearableLinkageEntity getAnyLinkage();

    /**
     * Insert or replace a linkage record.
     * REPLACE strategy ensures that re-linking updates the existing row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLinkage(WearableLinkageEntity linkage);

    /**
     * Update the sourceNodeId for an existing linkage identified by installationId.
     * Called when the wearable reconnects with a different nodeId.
     */
    @Query("UPDATE wearable_linkage SET nodeId = :newNodeId, last_seen_at_ms = :nowMs WHERE installation_id = :installationId")
    void updateNodeIdForInstallation(String installationId, String newNodeId, long nowMs);

    /**
     * Update the lastSeenAtMs timestamp when telemetry or device-info arrives.
     * Does NOT affect backendLinked status.
     */
    @Query("UPDATE wearable_linkage SET last_seen_at_ms = :nowMs WHERE nodeId = :nodeId")
    void touchLastSeen(String nodeId, long nowMs);

    /** Remove ALL linkage rows. Only call after a confirmed DELETE /api/v1/wearable/unlink. */
    @Query("DELETE FROM wearable_linkage")
    void clearAllLinkages();

    /** Remove a specific linkage by nodeId. */
    @Query("DELETE FROM wearable_linkage WHERE nodeId = :nodeId")
    void deleteLinkageByNodeId(String nodeId);
}
