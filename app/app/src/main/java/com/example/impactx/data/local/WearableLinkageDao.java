package com.example.impactx.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface WearableLinkageDao {
    @Query("SELECT * FROM wearable_linkage WHERE nodeId = :nodeId LIMIT 1")
    WearableLinkageEntity getLinkageByNodeId(String nodeId);

    @Query("SELECT * FROM wearable_linkage LIMIT 1")
    WearableLinkageEntity getAnyLinkage();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLinkage(WearableLinkageEntity linkage);

    @Query("DELETE FROM wearable_linkage")
    void clearAllLinkages();
}
