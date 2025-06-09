package com.am.mytodolistapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

import java.io.Serializable;

@Entity(tableName = "project_member_table")
public class ProjectMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "project_id")
    private String projectId;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "user_name")
    private String userName;

    @ColumnInfo(name = "user_email")
    private String userEmail;

    @ColumnInfo(name = "role")
    private String role; // "owner", "admin", "member"

    @ColumnInfo(name = "joined_at")
    private long joinedAt;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    @ColumnInfo(name = "invitation_status")
    private String invitationStatus; // "pending", "accepted", "declined"

    public ProjectMember() {
        this.joinedAt = System.currentTimeMillis();
        this.isActive = true;
        this.invitationStatus = "pending";
    }

    @Ignore
    public ProjectMember(String projectId, String userId, String userName, String userEmail, String role) {
        this();
        this.projectId = projectId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.role = role;
        if ("owner".equals(role)) {
            this.invitationStatus = "accepted";
        }
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getInvitationStatus() { return invitationStatus; }
    public void setInvitationStatus(String invitationStatus) { this.invitationStatus = invitationStatus; }
}