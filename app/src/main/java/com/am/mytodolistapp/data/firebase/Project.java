package com.am.mytodolistapp.data.firebase;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Project implements Serializable {
    private String projectId;
    private String projectName;
    private String description;
    private String ownerId; // 프로젝트 생성자
    private List<String> memberIds; // 참여 멤버 UID 목록
    private Map<String, String> memberRoles; // 멤버별 역할 (owner, member 등)
    private long createdAt;
    private long updatedAt;

    public Project() {} // Firebase용 기본 생성자

    public Project(String projectId, String projectName, String description, String ownerId) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public Map<String, String> getMemberRoles() { return memberRoles; }
    public void setMemberRoles(Map<String, String> memberRoles) { this.memberRoles = memberRoles; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}