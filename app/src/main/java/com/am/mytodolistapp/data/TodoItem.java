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

    // ========== 🆕 협업 관련 필드들 ==========
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

    @ColumnInfo(name = "priority", defaultValue = "MEDIUM")
    private String priority; // HIGH, MEDIUM, LOW

    // ========== 생성자들 ==========

    // 기본 생성자 (Room용)
    public TodoItem() {
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.isFromCollaboration = false;
        this.priority = "MEDIUM";
    }

    // 로컬 할 일 생성자
    @Ignore
    public TodoItem(String title) {
        this();
        this.title = title;
        this.isCompleted = false;
        this.isFromCollaboration = false;
    }

    // 협업 할 일 생성자
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

    // ========== 기본 필드 getters/setters ==========

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

    // ========== 위치 관련 getters/setters ==========

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

    // ========== 시간 관련 getters/setters ==========

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        updateTimestamp();
    }

    // ========== 🆕 협업 관련 getters/setters ==========

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

    public String getPriority() { return priority; }
    public void setPriority(String priority) {
        this.priority = priority != null ? priority : "MEDIUM";
        updateTimestamp();
    }

    // ========== 유틸리티 메서드들 ==========

    /**
     * 업데이트 시간 갱신 (협업 할 일이 아닌 경우에만)
     */
    private void updateTimestamp() {
        if (!isFromCollaboration) {
            this.updatedAt = System.currentTimeMillis();
        }
        // 협업 할 일의 경우 Firebase에서 관리하는 시간을 사용
    }

    /**
     * 할 일의 표시용 제목 반환 (프로젝트 정보 포함)
     */
    public String getDisplayTitle() {
        if (isFromCollaboration && projectName != null && !projectName.isEmpty()) {
            return "[" + projectName + "] " + title;
        }
        return title;
    }

    /**
     * 우선순위를 한국어로 반환
     */
    public String getPriorityDisplayText() {
        if (priority == null) return "보통";

        switch (priority.toUpperCase()) {
            case "HIGH": return "높음";
            case "MEDIUM": return "보통";
            case "LOW": return "낮음";
            default: return "보통";
        }
    }

    /**
     * 할 일 타입 확인 (로컬/협업)
     */
    public String getTypeDisplayText() {
        return isFromCollaboration ? "협업" : "개인";
    }

    /**
     * 완료 여부와 관계없이 할 일이 유효한지 확인
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty();
    }

    /**
     * 위치 기반 할 일인지 확인
     */
    public boolean hasLocation() {
        return locationEnabled && locationId != null && locationId > 0;
    }

    /**
     * 기한이 있는 할 일인지 확인
     */
    public boolean hasDueDate() {
        return dueDate != null;
    }

    /**
     * 기한이 지났는지 확인
     */
    public boolean isOverdue() {
        return dueDate != null && dueDate < System.currentTimeMillis() && !isCompleted;
    }

    /**
     * 오늘 기한인지 확인
     */
    public boolean isDueToday() {
        if (dueDate == null) return false;

        long today = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000;
        long startOfToday = today - (today % dayInMillis);
        long endOfToday = startOfToday + dayInMillis - 1;

        return dueDate >= startOfToday && dueDate <= endOfToday;
    }

    /**
     * 협업 할 일의 동기화 상태 확인
     */
    public boolean canSyncToFirebase() {
        return isFromCollaboration && firebaseTaskId != null && !firebaseTaskId.isEmpty();
    }

    // ========== Object 메서드 오버라이드 (🔧 개선됨) ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TodoItem todoItem = (TodoItem) o;

        // ID가 같은지 먼저 확인 (기본 식별자)
        if (id != todoItem.id) return false;

        // 주요 속성들이 같은지 확인 (DiffUtil이 변경사항을 감지하도록)
        if (isCompleted != todoItem.isCompleted) return false;
        if (isFromCollaboration != todoItem.isFromCollaboration) return false;
        if (locationEnabled != todoItem.locationEnabled) return false;
        if (Double.compare(todoItem.locationLatitude, locationLatitude) != 0) return false;
        if (Double.compare(todoItem.locationLongitude, locationLongitude) != 0) return false;
        if (Float.compare(todoItem.locationRadius, locationRadius) != 0) return false;
        if (createdAt != todoItem.createdAt) return false;
        if (updatedAt != todoItem.updatedAt) return false;

        // 문자열 속성들 비교
        if (!Objects.equals(title, todoItem.title)) return false;
        if (!Objects.equals(content, todoItem.content)) return false;
        if (!Objects.equals(categoryId, todoItem.categoryId)) return false;
        if (!Objects.equals(locationName, todoItem.locationName)) return false;
        if (!Objects.equals(locationId, todoItem.locationId)) return false;
        if (!Objects.equals(dueDate, todoItem.dueDate)) return false;
        if (!Objects.equals(projectId, todoItem.projectId)) return false;
        if (!Objects.equals(firebaseTaskId, todoItem.firebaseTaskId)) return false;
        if (!Objects.equals(projectName, todoItem.projectName)) return false;
        if (!Objects.equals(assignedTo, todoItem.assignedTo)) return false;
        if (!Objects.equals(createdBy, todoItem.createdBy)) return false;
        if (!Objects.equals(priority, todoItem.priority)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, title, content, isCompleted, categoryId, locationName,
                locationLatitude, locationLongitude, locationRadius, locationEnabled,
                locationId, createdAt, updatedAt, dueDate, isFromCollaboration,
                projectId, firebaseTaskId, projectName, assignedTo, createdBy, priority
        );
    }

    @Override
    public String toString() {
        return "TodoItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", isCompleted=" + isCompleted +
                ", isFromCollaboration=" + isFromCollaboration +
                ", projectName='" + projectName + '\'' +
                ", priority='" + priority + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}