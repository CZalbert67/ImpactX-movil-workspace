package com.example.impactx.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SessionEntity.class, AccidentEntity.class, WearSyncEventEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract AccidentDao accidentDao();
    public abstract WearSyncEventDao wearSyncEventDao();

    private static final androidx.room.migration.Migration MIGRATION_2_3 = new androidx.room.migration.Migration(2, 3) {
        @Override
        public void migrate(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `wear_sync_events` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` TEXT, `eventType` TEXT, `status` TEXT, `createdAtUtc` TEXT, `updatedAtUtc` TEXT, `backendEntityId` TEXT, `httpCode` INTEGER NOT NULL, `errorMessage` TEXT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_wear_sync_events_eventId` ON `wear_sync_events` (`eventId`)");
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
                    .addMigrations(MIGRATION_2_3)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
