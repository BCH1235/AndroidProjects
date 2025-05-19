package com.am.mytodolistapp.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration; // Migration 임포트
import androidx.sqlite.db.SupportSQLiteDatabase; // SupportSQLiteDatabase 임포트
import androidx.annotation.NonNull; // NonNull 임포트 (Migration 에서 사용)

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(entities = {TodoItem.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TodoDao todoDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);


    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 기존 todo_table 에 새로운 컬럼들을 추가하는 SQL 실행
            database.execSQL("ALTER TABLE todo_table ADD COLUMN estimated_time_minutes INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE todo_table ADD COLUMN actual_time_minutes INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE todo_table ADD COLUMN completion_timestamp INTEGER NOT NULL DEFAULT 0");

        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE todo_table ADD COLUMN location_name TEXT");
            database.execSQL("ALTER TABLE todo_table ADD COLUMN location_latitude REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE todo_table ADD COLUMN location_longitude REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE todo_table ADD COLUMN location_radius REAL NOT NULL DEFAULT 100.0");
            database.execSQL("ALTER TABLE todo_table ADD COLUMN location_enabled INTEGER NOT NULL DEFAULT 0");
        }
    };


    static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "todo_database")

                            .addMigrations(MIGRATION_1_2,MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}