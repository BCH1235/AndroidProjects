package com.am.mytodolistapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CollaborationTodoDao {

    @Insert
    void insert(CollaborationTodoItem todoItem);

    @Update
    void update(CollaborationTodoItem todoItem);

    @Delete
    void delete(CollaborationTodoItem todoItem);

    // 특정 프로젝트의 모든 할일 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE project_id = :projectId ORDER BY created_at DESC")
    LiveData<List<CollaborationTodoItem>> getProjectTodos(String projectId);

    // 특정 프로젝트의 완료되지 않은 할일 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE project_id = :projectId AND is_completed = 0 ORDER BY priority DESC, created_at DESC")
    LiveData<List<CollaborationTodoItem>> getProjectIncompleteTodos(String projectId);

    // 특정 프로젝트의 완료된 할일 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE project_id = :projectId AND is_completed = 1 ORDER BY completed_at DESC")
    LiveData<List<CollaborationTodoItem>> getProjectCompletedTodos(String projectId);

    // 특정 사용자에게 할당된 할일 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE assigned_to_id = :userId AND is_completed = 0 ORDER BY priority DESC, due_date ASC")
    LiveData<List<CollaborationTodoItem>> getAssignedTodos(String userId);

    // 특정 사용자가 생성한 할일 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE created_by_id = :userId ORDER BY created_at DESC")
    LiveData<List<CollaborationTodoItem>> getTodosByCreator(String userId);

    // 할일 ID로 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE todo_id = :todoId")
    LiveData<CollaborationTodoItem> getTodoById(String todoId);

    @Query("SELECT * FROM collaboration_todo_table WHERE todo_id = :todoId")
    CollaborationTodoItem getTodoByIdSync(String todoId);

    // 우선순위별 할일 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE project_id = :projectId AND priority = :priority AND is_completed = 0 ORDER BY created_at DESC")
    LiveData<List<CollaborationTodoItem>> getTodosByPriority(String projectId, int priority);

    // 기한별 할일 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE project_id = :projectId AND due_date BETWEEN :startDate AND :endDate ORDER BY due_date ASC")
    LiveData<List<CollaborationTodoItem>> getTodosByDateRange(String projectId, long startDate, long endDate);

    // 검색 기능
    @Query("SELECT * FROM collaboration_todo_table WHERE project_id = :projectId AND (title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%') ORDER BY created_at DESC")
    LiveData<List<CollaborationTodoItem>> searchProjectTodos(String projectId, String searchQuery);

    // 프로젝트의 할일 통계
    @Query("SELECT COUNT(*) FROM collaboration_todo_table WHERE project_id = :projectId")
    int getTotalTodoCount(String projectId);

    @Query("SELECT COUNT(*) FROM collaboration_todo_table WHERE project_id = :projectId AND is_completed = 1")
    int getCompletedTodoCount(String projectId);

    @Query("SELECT COUNT(*) FROM collaboration_todo_table WHERE project_id = :projectId AND is_completed = 0")
    int getIncompleteTodoCount(String projectId);

    // 기한이 지난 할일 조회
    @Query("SELECT * FROM collaboration_todo_table WHERE project_id = :projectId AND is_completed = 0 AND due_date < :currentTime ORDER BY due_date ASC")
    LiveData<List<CollaborationTodoItem>> getOverdueTodos(String projectId, long currentTime);

    @Query("DELETE FROM collaboration_todo_table")
    void deleteAllTodos();
}