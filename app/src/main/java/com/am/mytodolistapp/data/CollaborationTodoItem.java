package com.am.mytodolistapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

import java.io.Serializable;

@Entity(tableName = "collaboration_todo_table")
public class CollaborationTodoItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "todo_id")
    private String todoId; // Firebase의 고유 ID

    @ColumnInfo(name = "project_id")
    private String projectId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "is_completed")
    private boolean isCompleted;

    @ColumnInfo(name = "created_by_id")
    private String createdById;

    @ColumnInfo(name = "created_by_name")
    private String createdByName;

    @ColumnInfo(name = "assigned_to_id")
    private String assignedToId; // 할당받은 사용자 ID

    @ColumnInfo(name = "assigned_to_name")
    private String assignedToName; // 할당받은 사용자 이름

    @ColumnInfo(name = "priority")
    private int priority; // 1: 낮음, 2: 보통, 3: 높음

    @ColumnInfo(name = "due_date")
    private Long dueDate;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "completed_at")
    private Long completedAt;

    @ColumnInfo(name = "completed_by_id")
    private String completedById;

    @ColumnInfo(name = "completed_by_name")
    private String completedByName;

    public CollaborationTodoItem() {
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.isCompleted = false;
        this.priority = 2; // 기본 보통 우선순위
    }

    @Ignore
    public CollaborationTodoItem(String title, String projectId, String createdById, String createdByName) {
        this();
        this.title = title;
        this.projectId = projectId;
        this.createdById = createdById;
        this.createdByName = createdByName;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTodoId() { return todoId; }
    public void setTodoId(String todoId) { this.todoId = todoId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        this.updatedAt = System.currentTimeMillis();
        if (completed) {
            this.completedAt = System.currentTimeMillis();
        } else {
            this.completedAt = null;
            this.completedById = null;
            this.completedByName = null;
        }
    }

    public String getCreatedById() { return createdById; }
    public void setCreatedById(String createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public String getAssignedToId() { return assignedToId; }
    public void setAssignedToId(String assignedToId) {
        this.assignedToId = assignedToId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getPriority() { return priority; }
    public void setPriority(int priority) {
        this.priority = priority;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public String getCompletedById() { return completedById; }
    public void setCompletedById(String completedById) { this.completedById = completedById; }

    public String getCompletedByName() { return completedByName; }
    public void setCompletedByName(String completedByName) { this.completedByName = completedByName; }

    // 우선순위를 문자열로 반환하는 헬퍼 메서드
    public String getPriorityString() {
        switch (priority) {
            case 1: return "낮음";
            case 2: return "보통";
            case 3: return "높음";
            default: return "보통";
        }
    }

    // 할당 여부 확인 헬퍼 메서드
    public boolean isAssigned() {
        return assignedToId != null && !assignedToId.isEmpty();
    }
}