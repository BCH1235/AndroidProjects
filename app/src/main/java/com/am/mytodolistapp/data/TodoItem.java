package com.am.mytodolistapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

@Entity(tableName = "todo_table")
public class TodoItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title; // 할 일 제목

    @ColumnInfo(name = "content")
    private String content; // 할 일 내용

    @ColumnInfo(name = "is_completed", defaultValue = "false")
    private boolean isCompleted; // 완료 여부

    //새로 추가된 카테고리 관련 필드
    @ColumnInfo(name = "category_id")
    private Integer categoryId; // 카테고리 ID

    @ColumnInfo(name = "location_name")
    private String locationName;

    @ColumnInfo(name = "location_latitude")
    private double locationLatitude;

    @ColumnInfo(name = "location_longitude")
    private double locationLongitude;

    @ColumnInfo(name = "location_radius")
    private float locationRadius = 100f; // 기본값 100m

    @ColumnInfo(name = "location_enabled", defaultValue = "false")
    private boolean locationEnabled;

    @ColumnInfo(name = "location_id", defaultValue = "0")
    private int locationId;

    //새로 추가된 시간 관련 필드들
    @ColumnInfo(name = "created_at")
    private long createdAt; // 생성 시간

    @ColumnInfo(name = "updated_at")
    private long updatedAt; // 수정 시간

    // 새로 추가: 기한 날짜 필드
    @ColumnInfo(name = "due_date")
    private Long dueDate; // 기한 날짜 (nullable, YYYY-MM-DD 00:00:00의 timestamp)

    // Room이 사용할 기본 생성자
    public TodoItem() {
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
    }

    // 편의를 위한 생성자
    @Ignore
    public TodoItem(String title) {
        this.title = title;
        this.isCompleted = false;
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

    //새로 추가된 카테고리 관련 메소드
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
        this.updatedAt = System.currentTimeMillis();
    }

    // 기존 위치 관련 메소드
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

    public int getLocationId() { return locationId; }
    public void setLocationId(int locationId) { this.locationId = locationId; }

    //시간 관련 메소드
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    //기한 날짜 관련 메소드
    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = System.currentTimeMillis();
    }
}