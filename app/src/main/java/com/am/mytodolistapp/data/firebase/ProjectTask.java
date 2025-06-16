package com.am.mytodolistapp.data.firebase;

import java.io.Serializable;

// 'project_tasks' 컬렉션에 저장될 데이터 모델 클래스
//FirebaseRepository: 이 클래스의 객체를 사용하여 Firestore와 할 일 데이터를 주고 받는다.
//ProjectTaskListViewModel: UI에 표시할 프로젝트 할 일 데이터를 관리하기 위해 이 클래스를 사용
 //CollaborationSyncService: 이 ProjectTask 객체를 로컬 DB의 TodoItem 객체와 동기화하는 역할

public class ProjectTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskId; // 할 일 문서의 고유 ID.
    private String projectId; // 이 할 일이 속한 프로젝트의 ID
    private String title; // 할 일의 제목.
    private String content; // 할 일의 상세 내용.
    private boolean isCompleted; // 할 일의 완료 여부.
    private String assignedTo; // 담당자 UID
    private String createdBy; // 생성자 UID
    private Long dueDate;  // 할 일의 마감 기한

    private long createdAt; // 할 일이 생성된 시간
    private long updatedAt; // 할 일이 마지막으로 수정된 시간

    public ProjectTask() {} // Firebase용 기본 생성자

    public ProjectTask(String taskId, String projectId, String title, String createdBy) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.title = title;
        this.createdBy = createdBy;
        this.isCompleted = false; // 새로 생성된 할 일은 항상 '미완료' 상태로 시작한다.
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
        this.updatedAt = System.currentTimeMillis(); // 자동으로 갱신
    }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) {
        isCompleted = completed;
        this.updatedAt = System.currentTimeMillis(); // 자동으로 갱신
    }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
        this.updatedAt = System.currentTimeMillis(); // 자동으로 갱신
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = System.currentTimeMillis(); // 자동으로 갱신
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}