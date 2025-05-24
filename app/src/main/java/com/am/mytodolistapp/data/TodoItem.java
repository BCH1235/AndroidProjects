package com.am.mytodolistapp.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.threeten.bp.LocalDate;

@Entity(tableName = "todo_table")//데이터베이스의 '할 일' 항목 하나를 나타내는 데이터 구조
public class TodoItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title; // 할 일 제목

    @ColumnInfo(name = "content")
    private String content; // 할 일 내용

    @ColumnInfo(name = "is_completed", defaultValue = "false")
    private boolean isCompleted; // 완료 여부

    @ColumnInfo(name = "estimated_time_minutes", defaultValue = "0") // 예상 소요 시간 (분 단위)
    private int estimatedTimeMinutes;

    @ColumnInfo(name = "actual_time_minutes", defaultValue = "0") // 실제 소요 시간 (분 단위)
    private int actualTimeMinutes;

    @ColumnInfo(name = "completion_timestamp", defaultValue = "0") // 완료 시각
    private long completionTimestamp;

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

    @ColumnInfo(name = "due_date")
    private LocalDate dueDate; // 날짜

    public TodoItem() {
    }//기본 생성자


    public TodoItem(String title) {
        this.title = title;
        this.isCompleted = false; // 기본값 설정
        this.estimatedTimeMinutes = 0; // 기본값 명시적 설정 (선택 사항)
        this.actualTimeMinutes = 0;
        this.completionTimestamp = 0;
    }//제목을 받아 새 할 일 객체 생성

    //각 데이터 필드에 접근하기 위한 메소드들

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }//id 관련

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    } //제목 관련

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    } //내용 관련

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    } //완료 연부 관련

    public int getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }

    public void setEstimatedTimeMinutes(int estimatedTimeMinutes) {
        this.estimatedTimeMinutes = estimatedTimeMinutes;
    } //예상 시간 관련

    public int getActualTimeMinutes() {
        return actualTimeMinutes;
    }

    public void setActualTimeMinutes(int actualTimeMinutes) {
        this.actualTimeMinutes = actualTimeMinutes;
    } //실제 시간 관련

    public long getCompletionTimestamp() {
        return completionTimestamp;
    }

    public void setCompletionTimestamp(long completionTimestamp) {
        this.completionTimestamp = completionTimestamp;
    } //완료 시간 관련
    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public double getLocationLatitude() {
        return locationLatitude;
    }

    public void setLocationLatitude(double locationLatitude) {
        this.locationLatitude = locationLatitude;
    }

    public double getLocationLongitude() {
        return locationLongitude;
    }

    public void setLocationLongitude(double locationLongitude) {
        this.locationLongitude = locationLongitude;
    }

    public float getLocationRadius() {
        return locationRadius;
    }

    public void setLocationRadius(float locationRadius) {
        this.locationRadius = locationRadius;
    }

    public boolean isLocationEnabled() {
        return locationEnabled;
    }

    public void setLocationEnabled(boolean locationEnabled) {
        this.locationEnabled = locationEnabled;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

}