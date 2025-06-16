package com.am.mytodolistapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TodoDao {

    @Insert
    void insert(TodoItem todoItem);

    @Update
    void update(TodoItem todoItem);

    @Delete
    void delete(TodoItem todoItem);

    @Query("DELETE FROM todo_table")
    void deleteAllTodos();

    //카테고리 정보와 함께 모든 할 일 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "ORDER BY t.id DESC")
    LiveData<List<TodoWithCategoryInfo>> getAllTodosWithCategory();

    @Query("SELECT * FROM todo_table ORDER BY id DESC")
    LiveData<List<TodoItem>> getAllTodos();

    //특정 카테고리의 할 일들만 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.category_id = :categoryId " +
            "ORDER BY t.id DESC")
    LiveData<List<TodoWithCategoryInfo>> getTodosByCategoryWithInfo(int categoryId);

    //카테고리가 없는 할 일들 조회
    @Query("SELECT t.*, null as category_name, null as category_color " +
            "FROM todo_table t " +
            "WHERE t.category_id IS NULL " +
            "ORDER BY t.id DESC")
    LiveData<List<TodoWithCategoryInfo>> getTodosWithoutCategoryWithInfo();

    // 특정 ID 데이터 조회
    @Query("SELECT * FROM todo_table WHERE id = :id")
    LiveData<TodoItem> getTodoById(int id);

    @Query("SELECT * FROM todo_table WHERE id = :id")
    TodoItem getTodoByIdSync(int id);

    // 위치 기반 할 일 조회
    @Query("SELECT * FROM todo_table WHERE location_id = :locationId ORDER BY id DESC")
    LiveData<List<TodoItem>> getTodosByLocationId(int locationId);

    //완료된 할 일들만 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_completed = 1 " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getCompletedTodosWithCategory();

    //미완료된 할 일들만 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_completed = 0 " +
            "ORDER BY t.created_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getIncompleteTodosWithCategory();

    //특정 카테고리의 할 일 개수 조회
    @Query("SELECT COUNT(*) FROM todo_table WHERE category_id = :categoryId")
    int countTodosByCategory(int categoryId);

    //새로 추가: 검색 기능
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.title LIKE '%' || :searchQuery || '%' " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> searchTodosWithCategory(String searchQuery);

    //날짜 범위로 할 일 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.created_at BETWEEN :startDate AND :endDate " +
            "ORDER BY t.created_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getTodosByDateRangeWithCategory(long startDate, long endDate);

    // 새로 추가: 특정 기한 날짜의 할 일 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.due_date BETWEEN :startOfDay AND :endOfDay " +
            "ORDER BY t.due_date ASC")
    LiveData<List<TodoWithCategoryInfo>> getTodosByDueDateWithCategory(long startOfDay, long endOfDay);

    // 새로 추가: 기한이 없는 할 일들 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.due_date IS NULL " +
            "ORDER BY t.created_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getTodosWithoutDueDateWithCategory();

    // 새로 추가: 기한이 지난 할 일들 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.due_date < :currentTime " +
            "ORDER BY t.due_date DESC")
    LiveData<List<TodoWithCategoryInfo>> getOverdueTodosWithCategory(long currentTime);

    // 새로 추가: 오늘 기한인 할 일들 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.due_date BETWEEN :startOfToday AND :endOfToday " +
            "ORDER BY t.due_date ASC")
    LiveData<List<TodoWithCategoryInfo>> getTodayTodosWithCategory(long startOfToday, long endOfToday);

    // 새로 추가: 미래 기한인 할 일들 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.due_date > :endOfToday " +
            "ORDER BY t.due_date ASC")
    LiveData<List<TodoWithCategoryInfo>> getFutureTodosWithCategory(long endOfToday);

    // ========== 협업 관련 쿼리들 ==========

    @Query("SELECT * FROM todo_table WHERE firebase_task_id = :firebaseTaskId LIMIT 1")
    TodoItem getTodoByFirebaseTaskId(String firebaseTaskId);

    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 1 " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getCollaborationTodosWithCategory();

    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.project_id = :projectId " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getTodosByProjectWithCategory(String projectId);

    @Query("SELECT * FROM todo_table WHERE project_id = :projectId")
    List<TodoItem> getTodosByProjectIdSync(String projectId);

    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 0 OR t.is_from_collaboration IS NULL " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getLocalTodosWithCategory();

    @Query("DELETE FROM todo_table WHERE firebase_task_id = :firebaseTaskId")
    void deleteByFirebaseTaskId(String firebaseTaskId);

    @Query("DELETE FROM todo_table WHERE project_id = :projectId")
    void deleteAllTodosByProjectId(String projectId);

    @Query("SELECT COUNT(*) FROM todo_table WHERE is_from_collaboration = 1")
    int countCollaborationTodos();

    @Query("SELECT * FROM todo_table WHERE is_from_collaboration = 1 AND firebase_task_id IS NOT NULL")
    List<TodoItem> getAllCollaborationTodosSync();

    @Query("SELECT COUNT(*) FROM todo_table WHERE firebase_task_id = :firebaseTaskId")
    int countByFirebaseTaskId(String firebaseTaskId);

    @Query("SELECT project_id, " +
            "CAST(SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) as completion_rate " +
            "FROM todo_table " +
            "WHERE is_from_collaboration = 1 AND project_id IS NOT NULL " +
            "GROUP BY project_id")
    List<ProjectCompletionRate> getProjectCompletionRates();

    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 1 AND t.created_by = :userId " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getCollaborationTodosByCreator(String userId);

    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 1 AND t.assigned_to = :userId " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getCollaborationTodosByAssignee(String userId);

    @Query("DELETE FROM todo_table WHERE is_from_collaboration = 1")
    void deleteAllCollaborationTodos();

    // ===== Geofence 관련 쿼리들 (유지) =====
    @Query("SELECT * FROM todo_table WHERE location_enabled = 1 AND is_completed = 0")
    List<TodoItem> getActiveLocationBasedTodos();

    @Query("SELECT * FROM todo_table WHERE location_id = :locationId")
    List<TodoItem> getTodosByLocationIdSync(int locationId);

    @Query("DELETE FROM todo_table WHERE location_id = :locationId")
    void deleteAllTodosByLocationId(int locationId);

    @Query("SELECT COUNT(*) FROM todo_table WHERE location_id = :locationId")
    int countTodosByLocationId(int locationId);

    @Insert
    long insertAndGetId(TodoItem todoItem);

    // ========== 데이터 클래스들 ==========
    public static class ProjectCompletionRate {
        public String project_id;
        public float completion_rate;
    }

    class TodoWithCategoryInfo {
        // TodoItem의 모든 필드들
        public int id;
        public String title;
        public String content;
        public boolean is_completed;
        public Integer category_id;
        public String location_name;
        public double location_latitude;
        public double location_longitude;
        public float location_radius;
        public boolean location_enabled;
        public Integer location_id;
        public long created_at;
        public long updated_at;
        public Long due_date;

        // 협업 관련 필드들
        public boolean is_from_collaboration;
        public String project_id;
        public String firebase_task_id;
        public String project_name;
        public String assigned_to;
        public String created_by;


        // 카테고리 정보
        public String category_name;
        public String category_color;

        public TodoItem toTodoItem() {
            TodoItem todoItem = new TodoItem();
            todoItem.setId(this.id);
            todoItem.setTitle(this.title);
            todoItem.setContent(this.content);
            todoItem.setCompleted(this.is_completed);
            todoItem.setCategoryId(this.category_id);
            todoItem.setLocationName(this.location_name);
            todoItem.setLocationLatitude(this.location_latitude);
            todoItem.setLocationLongitude(this.location_longitude);
            todoItem.setLocationRadius(this.location_radius);
            todoItem.setLocationEnabled(this.location_enabled);
            todoItem.setLocationId(this.location_id);
            todoItem.setCreatedAt(this.created_at);
            todoItem.setUpdatedAt(this.updated_at);
            todoItem.setDueDate(this.due_date);

            todoItem.setFromCollaboration(this.is_from_collaboration);
            todoItem.setProjectId(this.project_id);
            todoItem.setFirebaseTaskId(this.firebase_task_id);
            todoItem.setProjectName(this.project_name);
            todoItem.setAssignedTo(this.assigned_to);
            todoItem.setCreatedBy(this.created_by);


            return todoItem;
        }
    }
}