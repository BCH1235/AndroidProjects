package com.am.mytodolistapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "location_table")
public class LocationItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name; // 위치 이름 (예: 집, 학교, 회사)

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "radius", defaultValue = "100.0")
    private float radius; // 지오펜스 반경

    @ColumnInfo(name = "is_enabled", defaultValue = "true")
    private boolean isEnabled; // 위치 알림 활성화 여부

    public LocationItem() {}

    public LocationItem(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = 100.0f;
        this.isEnabled = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
}