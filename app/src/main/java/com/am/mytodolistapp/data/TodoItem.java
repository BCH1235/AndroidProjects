package com.am.mytodolistapp.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "todo_table",
        foreignKeys = @ForeignKey(
                entity = LocationItem.class,
                parentColumns = "id",
                childColumns = "location_id",
                onDelete = ForeignKey.CASCADE
        ))
public class TodoItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "is_completed", defaultValue = "false")
    private boolean isCompleted;

    @ColumnInfo(name = "category_id")
    private Integer categoryId;

    @ColumnInfo(name = "location_name")
    private String locationName;

    @ColumnInfo(name = "location_latitude")
    private double locationLatitude;

    @ColumnInfo(name = "location_longitude")
    private double locationLongitude;

    @ColumnInfo(name = "location_radius")
    private float locationRadius = 100f;

    @ColumnInfo(name = "location_enabled", defaultValue = "false")
    private boolean locationEnabled;

    @ColumnInfo(name = "location_id", defaultValue = "0")
    private Integer locationId;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "due_date")
    private Long dueDate;

    // ========== 🆕 협업 관련 필드 추가 ==========
    @ColumnInfo(name = "is_from_collaboration", defaultValue = "false")
    private boolean isFromCollaboration; // 협업 할 일인지 구분

    @ColumnInfo(name = "project_id")
    private String projectId; // Firebase 프로젝트 ID

    @ColumnInfo(name = "firebase_task_id")
    private String firebaseTaskId; // Firebase 할 일 ID (동기화용)

    @ColumnInfo(name = "project_name")
    private String projectName; // 프로젝트 이름 (표시용)

    @ColumnInfo(name = "assigned_to")
    private String assignedTo; // 담당자 UID

    @ColumnInfo(name = "created_by")
    private String createdBy; // 생성자 UID

    @ColumnInfo(name = "priority")
    private String priority; // HIGH, MEDIUM, LOW

    // 기본 생성자
    public TodoItem() {
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.isFromCollaboration = false;
        this.priority = "MEDIUM";
    }

    // 편의 생성자
    @Ignore
    public TodoItem(String title) {
        this.title = title;
        this.isCompleted = false;
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.isFromCollaboration = false;
        this.priority = "MEDIUM";
    }

    // 🆕 협업 할 일 생성자
    @Ignore
    public TodoItem(String title, String projectId, String firebaseTaskId, String projectName) {
        this.title = title;
        this.isCompleted = false;
        this.projectId = projectId;
        this.firebaseTaskId = firebaseTaskId;
        this.projectName = projectName;
        this.isFromCollaboration = true;
        this.priority = "MEDIUM";
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
    }

    // 기존 getters/setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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
        isCompleted = completed;
        this.updatedAt = System.currentTimeMillis();
    }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
        this.updatedAt = System.currentTimeMillis();
    }

    // 위치 관련 메소드들 (기존 유지)
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public double getLocationLatitude() { return locationLatitude; }
    public void setLocationLatitude(double locationLatitude) { this.locationLatitude = locationLatitude; }

    public double getLocationLongitude() { return locationLongitude; }
    public void setLocationLongitude(double locationLongitude) { this.locationLongitude = locationLongitude; }

    public float getLocationRadius() { return locationRadius; }
    public void setLocationRadius(float locationRadius) { this.locationRadius = locationRadius; }

    public boolean isLocationEnabled() { return locationEnabled; }
    public void setLocationEnabled(boolean locationEnabled) { this.locationEnabled = locationEnabled; }

    public Integer getLocationId() { return locationId; }
    public void setLocationId(Integer locationId) { this.locationId = locationId; }

    // 시간 관련 메소드
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = System.currentTimeMillis();
    }

    // ========== 🆕 협업 관련 getters/setters ==========
    public boolean isFromCollaboration() { return isFromCollaboration; }
    public void setFromCollaboration(boolean fromCollaboration) {
        isFromCollaboration = fromCollaboration;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) {
        this.projectId = projectId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getFirebaseTaskId() { return firebaseTaskId; }
    public void setFirebaseTaskId(String firebaseTaskId) {
        this.firebaseTaskId = firebaseTaskId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPriority() { return priority; }
    public void setPriority(String priority) {
        this.priority = priority;
        this.updatedAt = System.currentTimeMillis();
    }
}