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

// 1. 데이터베이스 버전을 9로 올립니다.
@Database(entities = {TodoItem.class, LocationItem.class, CategoryItem.class}, version = 9, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TodoDao todoDao();
    public abstract LocationDao locationDao();
    public abstract CategoryDao categoryDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // --- 기존 마이그레이션 (변경 없음) ---
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
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
            database.execSQL("CREATE TABLE IF NOT EXISTS `location_table` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT, " +
                    "`latitude` REAL NOT NULL, " +
                    "`longitude` REAL NOT NULL, " +
                    "`radius` REAL NOT NULL DEFAULT 100.0, " +
                    "`is_enabled` INTEGER NOT NULL DEFAULT 'true')");
            database.execSQL("ALTER TABLE todo_table ADD COLUMN location_id INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
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

            long currentTime = System.currentTimeMillis();
            database.execSQL("INSERT INTO category_table (name, color, is_default, created_at, order_index) VALUES " +
                    "('업무', '#FF6200EE', 1, " + currentTime + ", 1), " +
                    "('개인', '#FF03DAC6', 1, " + currentTime + ", 2), " +
                    "('쇼핑', '#FFFF5722', 1, " + currentTime + ", 3), " +
                    "('건강', '#FF4CAF50', 1, " + currentTime + ", 4), " +
                    "('학습', '#FF2196F3', 1, " + currentTime + ", 5)");
            database.execSQL("UPDATE todo_table SET created_at = " + currentTime + ", updated_at = " + currentTime + " WHERE created_at = 0");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE todo_table ADD COLUMN due_date INTEGER");
        }
    };

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // no-op migration
        }
    };

    // 2. location_id를 NULL로 가질 수 있도록 스키마를 변경하는 마이그레이션을 추가합니다.
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. 기존 테이블을 임시 이름으로 변경합니다.
            database.execSQL("ALTER TABLE todo_table RENAME TO todo_table_old");

            // 2. 새로운 스키마로 테이블을 다시 생성합니다. location_id가 NOT NULL이 아니고, 기본값도 없습니다.
            database.execSQL("CREATE TABLE `todo_table` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT, `content` TEXT, `is_completed` INTEGER NOT NULL, " +
                    "`category_id` INTEGER, `location_name` TEXT, `location_latitude` REAL NOT NULL, " +
                    "`location_longitude` REAL NOT NULL, `location_radius` REAL NOT NULL, " +
                    "`location_enabled` INTEGER NOT NULL, `location_id` INTEGER, " + // NULL을 허용하도록 변경
                    "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `due_date` INTEGER, " +
                    "FOREIGN KEY(`location_id`) REFERENCES `location_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");

            // 3. 임시 테이블의 데이터를 새 테이블로 복사합니다. 이때 location_id가 0이었던 값들은 NULL로 변환합니다.
            database.execSQL("INSERT INTO todo_table (id, title, content, is_completed, category_id, location_name, " +
                    "location_latitude, location_longitude, location_radius, location_enabled, location_id, " +
                    "created_at, updated_at, due_date) " +
                    "SELECT id, title, content, is_completed, category_id, location_name, " +
                    "location_latitude, location_longitude, location_radius, location_enabled, " +
                    "CASE WHEN location_id = 0 THEN NULL ELSE location_id END, " + // 0을 NULL로 변환하는 로직
                    "created_at, updated_at, due_date FROM todo_table_old");

            // 4. 임시 테이블을 삭제합니다.
            database.execSQL("DROP TABLE todo_table_old");

            // 5. 외래 키에 대한 인덱스를 다시 생성합니다. (성능 향상에 도움)
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_table_location_id` ON `todo_table` (`location_id`)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "todo_database")
                            // 3. fallbackToDestructiveMigration()을 제거하여 마이그레이션이 실행되도록 합니다.
                            // .fallbackToDestructiveMigration()
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5,
                                    MIGRATION_5_6,
                                    MIGRATION_6_7,
                                    MIGRATION_7_8,
                                    MIGRATION_8_9 // 4. 새로운 마이그레이션을 추가합니다.
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}