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


@Database(entities = {TodoItem.class, LocationItem.class, CategoryItem.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TodoDao todoDao();
    public abstract LocationDao locationDao();

    public abstract CategoryDao categoryDao();

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

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            database.execSQL("CREATE TABLE IF NOT EXISTS `category_table` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT, " +
                    "`color` TEXT, " +
                    "`icon` TEXT, " +
                    "`is_default` INTEGER NOT NULL DEFAULT 0, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`order_index` INTEGER NOT NULL DEFAULT 0)");


            database.execSQL("ALTER TABLE todo_table ADD COLUMN category_id INTEGER");


            database.execSQL("ALTER TABLE todo_table ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE todo_table ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0");

            //기본 카테고리들 삽입
            long currentTime = System.currentTimeMillis();

            database.execSQL("INSERT INTO category_table (name, color, is_default, created_at, order_index) VALUES " +
                    "('업무', '#FF6200EE', 1, " + currentTime + ", 1), " +
                    "('개인', '#FF03DAC6', 1, " + currentTime + ", 2), " +
                    "('쇼핑', '#FFFF5722', 1, " + currentTime + ", 3), " +
                    "('건강', '#FF4CAF50', 1, " + currentTime + ", 4), " +
                    "('학습', '#FF2196F3', 1, " + currentTime + ", 5)");

            // 5. 기존 할 일들의 시간 정보 업데이트
            database.execSQL("UPDATE todo_table SET created_at = " + currentTime + ", updated_at = " + currentTime + " WHERE created_at = 0");
        }
    };


    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "todo_database")
                            .fallbackToDestructiveMigration()
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}