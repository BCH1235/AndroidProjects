package com.am.mytodolistapp.data.firebase;

import java.io.Serializable;

public class ProjectTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskId;
    private String projectId;
    private String title;
    private String content;
    private boolean isCompleted;
    private String assignedTo; // 담당자 UID
    private String createdBy; // 생성자 UID
    private Long dueDate;

    private long createdAt;
    private long updatedAt;

    public ProjectTask() {} // Firebase용 기본 생성자

    public ProjectTask(String taskId, String projectId, String title, String createdBy) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.title = title;
        this.createdBy = createdBy;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

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

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}