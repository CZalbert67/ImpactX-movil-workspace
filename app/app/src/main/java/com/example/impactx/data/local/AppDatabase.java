package com.example.impactx.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {
        SessionEntity.class,
        AccidentEntity.class,
        WearSyncEventEntity.class,
        WearableLinkageEntity.class
    },
    version = 5,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract AccidentDao accidentDao();
    public abstract WearSyncEventDao wearSyncEventDao();
    public abstract WearableLinkageDao wearableLinkageDao();

    // ── Migration 2 → 3: initial wear_sync_events table ──────────────────────
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `wear_sync_events` ("
                + "`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`eventId` TEXT, `eventType` TEXT, `status` TEXT, "
                + "`createdAtUtc` TEXT, `updatedAtUtc` TEXT, `backendEntityId` TEXT, "
                + "`httpCode` INTEGER NOT NULL, `errorMessage` TEXT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_wear_sync_events_eventId` "
                + "ON `wear_sync_events` (`eventId`)");
        }
    };

    // ── Migration 3 → 4: wearable_linkage + new wear_sync_events schema ──────
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create wearable_linkage table (v4 — without installationId)
            database.execSQL("CREATE TABLE IF NOT EXISTS `wearable_linkage` ("
                + "`nodeId` TEXT NOT NULL PRIMARY KEY, "
                + "`backendDeviceId` TEXT, `nombre` TEXT, `modelo` TEXT, "
                + "`fabricante` TEXT, `estado` TEXT, `linkedAt` INTEGER NOT NULL)");

            // Rename old wear_sync_events table
            database.execSQL("ALTER TABLE `wear_sync_events` RENAME TO `wear_sync_events_old`");

            // Create new wear_sync_events with v4 schema
            database.execSQL("CREATE TABLE IF NOT EXISTS `wear_sync_events` ("
                + "`eventId` TEXT NOT NULL PRIMARY KEY, "
                + "`sourceNodeId` TEXT, `eventType` TEXT, `status` TEXT, "
                + "`backendTripId` TEXT, `httpCode` INTEGER, "
                + "`errorMessage` TEXT, `createdAt` TEXT, `updatedAt` TEXT)");

            // Copy data from old table
            database.execSQL("INSERT INTO `wear_sync_events` "
                + "(`eventId`, `sourceNodeId`, `eventType`, `status`, `backendTripId`, "
                + "`httpCode`, `errorMessage`, `createdAt`, `updatedAt`) "
                + "SELECT `eventId`, NULL, `eventType`, `status`, `backendEntityId`, "
                + "`httpCode`, `errorMessage`, `createdAtUtc`, `updatedAtUtc` "
                + "FROM `wear_sync_events_old` WHERE `eventId` IS NOT NULL");

            database.execSQL("DROP TABLE `wear_sync_events_old`");
        }
    };

    // ── Migration 4 → 5: add installation_id and last_seen_at_ms to wearable_linkage ──
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add installationId column (stable wearable UUID, defaults to empty string)
            database.execSQL(
                "ALTER TABLE `wearable_linkage` ADD COLUMN `installation_id` TEXT NOT NULL DEFAULT ''");
            // Add lastSeenAtMs column (epoch ms of last telemetry/device-info received)
            database.execSQL(
                "ALTER TABLE `wearable_linkage` ADD COLUMN `last_seen_at_ms` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "impactx_db"
                    )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
