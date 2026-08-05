package com.example.impactx.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SessionEntity.class, AccidentEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract AccidentDao accidentDao();

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
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
