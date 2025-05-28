package com.am.mytodolistapp.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(entities = {TodoItem.class, LocationItem.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TodoDao todoDao();
    public abstract LocationDao locationDao();

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
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // location_table 생성
            database.execSQL("CREATE TABLE IF NOT EXISTS `location_table` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT, " +
                    "`latitude` REAL NOT NULL, " +
                    "`longitude` REAL NOT NULL, " +
                    "`radius` REAL NOT NULL DEFAULT 100.0, " +
                    "`is_enabled` INTEGER NOT NULL DEFAULT 'true')");

            // todo_table에 location_id 컬럼 추가
            database.execSQL("ALTER TABLE todo_table ADD COLUMN location_id INTEGER NOT NULL DEFAULT 0");
        }
    };
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 시간 관련 컬럼들 제거
            database.execSQL("CREATE TABLE todo_table_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT, " +
                    "content TEXT, " +
                    "is_completed INTEGER NOT NULL DEFAULT 0, " +
                    "location_name TEXT, " +
                    "location_latitude REAL NOT NULL DEFAULT 0.0, " +
                    "location_longitude REAL NOT NULL DEFAULT 0.0, " +
                    "location_radius REAL NOT NULL DEFAULT 100.0, " +
                    "location_enabled INTEGER NOT NULL DEFAULT 0, " +
                    "location_id INTEGER NOT NULL DEFAULT 0)");

            database.execSQL("INSERT INTO todo_table_new (id, title, content, is_completed, location_name, location_latitude, location_longitude, location_radius, location_enabled, location_id) " +
                    "SELECT id, title, content, is_completed, location_name, location_latitude, location_longitude, location_radius, location_enabled, location_id FROM todo_table");

            database.execSQL("DROP TABLE todo_table");
            database.execSQL("ALTER TABLE todo_table_new RENAME TO todo_table");
        }
    };

    // getDatabase 메서드에서 마이그레이션 추가:
    public static AppDatabase getDatabase(final Context context) {  // public 추가
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "todo_database")
                            .fallbackToDestructiveMigration() // 임시
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}