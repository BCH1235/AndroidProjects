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

    // ========== 🆕 협업 관련 쿼리들 ==========

    // Firebase 할 일 ID로 기존 TodoItem 찾기 (동기화용)
    @Query("SELECT * FROM todo_table WHERE firebase_task_id = :firebaseTaskId LIMIT 1")
    TodoItem getTodoByFirebaseTaskId(String firebaseTaskId);

    // 협업 할 일들만 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 1 " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getCollaborationTodosWithCategory();

    // 특정 프로젝트의 할 일들 조회 (LiveData)
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.project_id = :projectId " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getTodosByProjectWithCategory(String projectId);

    // 🆕 특정 프로젝트의 할 일들 조회 (동기화용 - 동기적)
    @Query("SELECT * FROM todo_table WHERE project_id = :projectId")
    List<TodoItem> getTodosByProjectIdSync(String projectId);

    // 로컬 할 일들만 조회 (협업 제외)
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 0 OR t.is_from_collaboration IS NULL " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getLocalTodosWithCategory();

    // Firebase 할 일 ID로 삭제
    @Query("DELETE FROM todo_table WHERE firebase_task_id = :firebaseTaskId")
    void deleteByFirebaseTaskId(String firebaseTaskId);

    // 특정 프로젝트의 모든 할 일 삭제
    @Query("DELETE FROM todo_table WHERE project_id = :projectId")
    void deleteAllTodosByProjectId(String projectId);

    // 협업 할 일 여부로 개수 조회
    @Query("SELECT COUNT(*) FROM todo_table WHERE is_from_collaboration = 1")
    int countCollaborationTodos();

    // 동기화가 필요한 협업 할 일들 조회 (업데이트 확인용)
    @Query("SELECT * FROM todo_table WHERE is_from_collaboration = 1 AND firebase_task_id IS NOT NULL")
    List<TodoItem> getAllCollaborationTodosSync();

    // Firebase 할 일 ID 존재 여부 확인
    @Query("SELECT COUNT(*) FROM todo_table WHERE firebase_task_id = :firebaseTaskId")
    int countByFirebaseTaskId(String firebaseTaskId);

    // 프로젝트별 완료율 계산용 쿼리
    @Query("SELECT project_id, " +
            "CAST(SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) as completion_rate " +
            "FROM todo_table " +
            "WHERE is_from_collaboration = 1 AND project_id IS NOT NULL " +
            "GROUP BY project_id")
    List<ProjectCompletionRate> getProjectCompletionRates();

    // 🆕 특정 사용자가 생성한 협업 할 일들 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 1 AND t.created_by = :userId " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getCollaborationTodosByCreator(String userId);

    // 🆕 특정 사용자에게 할당된 협업 할 일들 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 1 AND t.assigned_to = :userId " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getCollaborationTodosByAssignee(String userId);

    // 🆕 우선순위별 협업 할 일들 조회
    @Query("SELECT t.*, c.name as category_name, c.color as category_color " +
            "FROM todo_table t " +
            "LEFT JOIN category_table c ON t.category_id = c.id " +
            "WHERE t.is_from_collaboration = 1 AND t.priority = :priority " +
            "ORDER BY t.updated_at DESC")
    LiveData<List<TodoWithCategoryInfo>> getCollaborationTodosByPriority(String priority);

    // 🆕 모든 협업 할 일 삭제 (로그아웃 시 사용)
    @Query("DELETE FROM todo_table WHERE is_from_collaboration = 1")
    void deleteAllCollaborationTodos();

    // ===== 🚀 Geofence 기능을 위해 기존에 있던 메서드들 =====

    // 활성화된 위치 기반 할 일들을 가져오는 메서드 (앱 시작 시 Geofence 등록용)
    @Query("SELECT * FROM todo_table WHERE location_enabled = 1 AND is_completed = 0")
    List<TodoItem> getActiveLocationBasedTodos();

    // 특정 위치의 할 일들을 동기적으로 가져오는 메서드
    @Query("SELECT * FROM todo_table WHERE location_id = :locationId")
    List<TodoItem> getTodosByLocationIdSync(int locationId);

    @Query("DELETE FROM todo_table WHERE location_id = :locationId")
    void deleteAllTodosByLocationId(int locationId);

    @Query("SELECT COUNT(*) FROM todo_table WHERE location_id = :locationId")
    int countTodosByLocationId(int locationId);

    // 할 일을 삽입하고 ID를 반환하는 메서드
    @Insert
    long insertAndGetId(TodoItem todoItem);

    // ========== 데이터 클래스들 ==========

    // 프로젝트 완료율 결과를 담을 데이터 클래스
    public static class ProjectCompletionRate {
        public String project_id;
        public float completion_rate;
    }

    // JOIN 결과를 담을 데이터 클래스
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
        public Integer location_id; // Integer로 변경 (null 허용)
        public long created_at;
        public long updated_at;
        public Long due_date;

        // 🆕 협업 관련 필드들
        public boolean is_from_collaboration;
        public String project_id;
        public String firebase_task_id;
        public String project_name;
        public String assigned_to;
        public String created_by;
        public String priority;

        // 카테고리 정보
        public String category_name;
        public String category_color;

        // TodoItem으로 변환하는 메서드
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

            // 🆕 협업 관련 필드들 설정
            todoItem.setFromCollaboration(this.is_from_collaboration);
            todoItem.setProjectId(this.project_id);
            todoItem.setFirebaseTaskId(this.firebase_task_id);
            todoItem.setProjectName(this.project_name);
            todoItem.setAssignedTo(this.assigned_to);
            todoItem.setCreatedBy(this.created_by);
            todoItem.setPriority(this.priority);

            return todoItem;
        }
    }
}