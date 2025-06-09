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

@Database(entities = {
        TodoItem.class,
        LocationItem.class,
        CategoryItem.class,
        CollaborationProject.class,
        ProjectMember.class,
        CollaborationTodoItem.class
}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TodoDao todoDao();
    public abstract LocationDao locationDao();
    public abstract CategoryDao categoryDao();
    public abstract CollaborationProjectDao collaborationProjectDao();
    public abstract ProjectMemberDao projectMemberDao();
    public abstract CollaborationTodoDao collaborationTodoDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

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
            // 협업 프로젝트 테이블 생성
            database.execSQL("CREATE TABLE IF NOT EXISTS `collaboration_project_table` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`project_id` TEXT, " +
                    "`name` TEXT, " +
                    "`description` TEXT, " +
                    "`owner_id` TEXT, " +
                    "`owner_name` TEXT, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL, " +
                    "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                    "`member_count` INTEGER NOT NULL DEFAULT 1)");
        }
    };

    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 프로젝트 멤버 테이블 생성
            database.execSQL("CREATE TABLE IF NOT EXISTS `project_member_table` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`project_id` TEXT, " +
                    "`user_id` TEXT, " +
                    "`user_name` TEXT, " +
                    "`user_email` TEXT, " +
                    "`role` TEXT, " +
                    "`joined_at` INTEGER NOT NULL, " +
                    "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                    "`invitation_status` TEXT)");
        }
    };

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 협업 할일 테이블 생성
            database.execSQL("CREATE TABLE IF NOT EXISTS `collaboration_todo_table` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`todo_id` TEXT, " +
                    "`project_id` TEXT, " +
                    "`title` TEXT, " +
                    "`content` TEXT, " +
                    "`is_completed` INTEGER NOT NULL DEFAULT 0, " +
                    "`created_by_id` TEXT, " +
                    "`created_by_name` TEXT, " +
                    "`assigned_to_id` TEXT, " +
                    "`assigned_to_name` TEXT, " +
                    "`priority` INTEGER NOT NULL DEFAULT 2, " +
                    "`due_date` INTEGER, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL, " +
                    "`completed_at` INTEGER, " +
                    "`completed_by_id` TEXT, " +
                    "`completed_by_name` TEXT)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "todo_database")
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_5_6,
                                    MIGRATION_6_7,
                                    MIGRATION_7_8,
                                    MIGRATION_8_9,
                                    MIGRATION_9_10
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}