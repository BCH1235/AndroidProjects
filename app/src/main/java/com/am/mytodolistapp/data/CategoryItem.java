package com.am.mytodolistapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

@Entity(tableName = "category_table")
public class CategoryItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name; // 카테고리 이름

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "icon")
    private String icon;

    @ColumnInfo(name = "is_default", defaultValue = "false")
    private boolean isDefault; // 기본 제공 카테고리 여부

    @ColumnInfo(name = "created_at")
    private long createdAt; // 생성 시간

    @ColumnInfo(name = "order_index", defaultValue = "0")
    private int orderIndex; // 정렬 순서

    // Room이 사용할 기본 생성자 (경고 없음)
    public CategoryItem() {
        this.createdAt = System.currentTimeMillis();
    }

    // 편의를 위한 생성자들
    @Ignore
    public CategoryItem(String name, String color) {
        this.name = name;
        this.color = color;
        this.isDefault = false;
        this.createdAt = System.currentTimeMillis();
        this.orderIndex = 0;
    }

    @Ignore
    public CategoryItem(String name, String color, boolean isDefault) {
        this.name = name;
        this.color = color;
        this.isDefault = isDefault;
        this.createdAt = System.currentTimeMillis();
        this.orderIndex = 0;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    // 정적 팩토리 메서드
    public static CategoryItem createDefaultCategory(String name, String color, int orderIndex) {
        CategoryItem category = new CategoryItem(name, color, true);
        category.setOrderIndex(orderIndex);
        return category;
    }
}