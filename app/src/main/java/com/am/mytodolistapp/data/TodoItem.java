package com.am.mytodolistapp.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

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

    @ColumnInfo(name = "location_id")
    private Integer locationId;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "due_date")
    private Long dueDate;

    // ========== 협업 관련 필드들 ==========
    @ColumnInfo(name = "is_from_collaboration", defaultValue = "false")
    private boolean isFromCollaboration;

    @ColumnInfo(name = "project_id")
    private String projectId;

    @ColumnInfo(name = "firebase_task_id")
    private String firebaseTaskId;

    @ColumnInfo(name = "project_name")
    private String projectName;

    @ColumnInfo(name = "assigned_to")
    private String assignedTo;

    @ColumnInfo(name = "created_by")
    private String createdBy;

    // ========== 생성자들 ==========

    public TodoItem() {
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.isFromCollaboration = false;
    }

    @Ignore
    public TodoItem(String title) {
        this();
        this.title = title;
        this.isCompleted = false;
        this.isFromCollaboration = false;
    }

    @Ignore
    public TodoItem(String title, String projectId, String firebaseTaskId, String projectName) {
        this();
        this.title = title;
        this.isCompleted = false;
        this.projectId = projectId;
        this.firebaseTaskId = firebaseTaskId;
        this.projectName = projectName;
        this.isFromCollaboration = true;
    }

    // ========== Getters and Setters ==========

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        updateTimestamp();
    }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        updateTimestamp();
    }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) {
        isCompleted = completed;
        updateTimestamp();
    }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
        updateTimestamp();
    }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) {
        this.locationName = locationName;
        updateTimestamp();
    }

    public double getLocationLatitude() { return locationLatitude; }
    public void setLocationLatitude(double locationLatitude) {
        this.locationLatitude = locationLatitude;
        updateTimestamp();
    }

    public double getLocationLongitude() { return locationLongitude; }
    public void setLocationLongitude(double locationLongitude) {
        this.locationLongitude = locationLongitude;
        updateTimestamp();
    }

    public float getLocationRadius() { return locationRadius; }
    public void setLocationRadius(float locationRadius) {
        this.locationRadius = locationRadius;
        updateTimestamp();
    }

    public boolean isLocationEnabled() { return locationEnabled; }
    public void setLocationEnabled(boolean locationEnabled) {
        this.locationEnabled = locationEnabled;
        updateTimestamp();
    }

    public Integer getLocationId() { return locationId; }
    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
        updateTimestamp();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        updateTimestamp();
    }

    public boolean isFromCollaboration() { return isFromCollaboration; }
    public void setFromCollaboration(boolean fromCollaboration) {
        isFromCollaboration = fromCollaboration;
        updateTimestamp();
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) {
        this.projectId = projectId;
        updateTimestamp();
    }

    public String getFirebaseTaskId() { return firebaseTaskId; }
    public void setFirebaseTaskId(String firebaseTaskId) {
        this.firebaseTaskId = firebaseTaskId;
        updateTimestamp();
    }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
        updateTimestamp();
    }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
        updateTimestamp();
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        updateTimestamp();
    }

    // ========== 유틸리티 메서드들 ==========

    private void updateTimestamp() {
        if (!isFromCollaboration) {
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public String getDisplayTitle() {
        if (isFromCollaboration && projectName != null && !projectName.isEmpty()) {
            return "[" + projectName + "] " + title;
        }
        return title;
    }

    public String getTypeDisplayText() {
        return isFromCollaboration ? "협업" : "개인";
    }

    public boolean isValid() {
        return title != null && !title.trim().isEmpty();
    }

    public boolean hasLocation() {
        return locationEnabled && locationId != null && locationId > 0;
    }

    public boolean hasDueDate() {
        return dueDate != null;
    }

    public boolean isOverdue() {
        return dueDate != null && dueDate < System.currentTimeMillis() && !isCompleted;
    }

    public boolean isDueToday() {
        if (dueDate == null) return false;
        long today = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000;
        long startOfToday = today - (today % dayInMillis);
        long endOfToday = startOfToday + dayInMillis - 1;
        return dueDate >= startOfToday && dueDate <= endOfToday;
    }

    public boolean canSyncToFirebase() {
        return isFromCollaboration && firebaseTaskId != null && !firebaseTaskId.isEmpty();
    }

    // ========== Object 메서드 오버라이드 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TodoItem todoItem = (TodoItem) o;
        // DiffUtil이 변경을 감지할 수 있도록 주요 필드들을 비교합니다.
        return id == todoItem.id &&
                isCompleted == todoItem.isCompleted &&
                isFromCollaboration == todoItem.isFromCollaboration &&
                Objects.equals(title, todoItem.title) &&
                Objects.equals(content, todoItem.content) &&
                Objects.equals(categoryId, todoItem.categoryId) &&
                Objects.equals(dueDate, todoItem.dueDate) &&
                Objects.equals(updatedAt, todoItem.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, isCompleted, categoryId, dueDate, updatedAt, isFromCollaboration);
    }

    @Override
    public String toString() {
        return "TodoItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", isCompleted=" + isCompleted +
                ", isFromCollaboration=" + isFromCollaboration +
                ", projectName='" + projectName + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}