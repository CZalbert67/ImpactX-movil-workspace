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
    version = 4,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract AccidentDao accidentDao();
    public abstract WearSyncEventDao wearSyncEventDao();
    public abstract WearableLinkageDao wearableLinkageDao();

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `wear_sync_events` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` TEXT, `eventType` TEXT, `status` TEXT, `createdAtUtc` TEXT, `updatedAtUtc` TEXT, `backendEntityId` TEXT, `httpCode` INTEGER NOT NULL, `errorMessage` TEXT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_wear_sync_events_eventId` ON `wear_sync_events` (`eventId`)");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create wearable_linkage table
            database.execSQL("CREATE TABLE IF NOT EXISTS `wearable_linkage` (`nodeId` TEXT NOT NULL PRIMARY KEY, `backendDeviceId` TEXT, `nombre` TEXT, `modelo` TEXT, `fabricante` TEXT, `estado` TEXT, `linkedAt` INTEGER NOT NULL)");

            // Rename old wear_sync_events table
            database.execSQL("ALTER TABLE `wear_sync_events` RENAME TO `wear_sync_events_old`");

            // Create new wear_sync_events table according to version 4 schema
            database.execSQL("CREATE TABLE IF NOT EXISTS `wear_sync_events` (`eventId` TEXT NOT NULL PRIMARY KEY, `sourceNodeId` TEXT, `eventType` TEXT, `status` TEXT, `backendTripId` TEXT, `httpCode` INTEGER, `errorMessage` TEXT, `createdAt` TEXT, `updatedAt` TEXT)");

            // Copy data from old to new table, setting sourceNodeId to NULL
            database.execSQL("INSERT INTO `wear_sync_events` (`eventId`, `sourceNodeId`, `eventType`, `status`, `backendTripId`, `httpCode`, `errorMessage`, `createdAt`, `updatedAt`)" +
                    " SELECT `eventId`, NULL, `eventType`, `status`, `backendEntityId`, `httpCode`, `errorMessage`, `createdAtUtc`, `updatedAtUtc` FROM `wear_sync_events_old` WHERE `eventId` IS NOT NULL");

            // Drop temporary table
            database.execSQL("DROP TABLE `wear_sync_events_old`");
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
