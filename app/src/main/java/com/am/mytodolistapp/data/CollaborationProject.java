package com.am.mytodolistapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

import java.io.Serializable;

@Entity(tableName = "collaboration_project_table")
public class CollaborationProject implements Serializable {

    private static final long serialVersionUID = 1L;

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "project_id")
    private String projectId; // Firebase의 고유 ID

    @ColumnInfo(name = "name")
    private String name; // 프로젝트 이름

    @ColumnInfo(name = "description")
    private String description; // 프로젝트 설명

    @ColumnInfo(name = "owner_id")
    private String ownerId; // 프로젝트 생성자 ID

    @ColumnInfo(name = "owner_name")
    private String ownerName; // 프로젝트 생성자 이름

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "is_active")
    private boolean isActive; // 프로젝트 활성화 상태

    @ColumnInfo(name = "member_count")
    private int memberCount; // 멤버 수

    public CollaborationProject() {
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.isActive = true;
        this.memberCount = 1;
    }

    @Ignore
    public CollaborationProject(String name, String description, String ownerId, String ownerName) {
        this();
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}