package com.am.mytodolistapp.data.firebase;

public class ProjectInvitation {
    private String invitationId;
    private String projectId;
    private String projectName;
    private String inviterUid;
    private String inviterEmail;
    private String inviteeEmail;
    private String status; // PENDING, ACCEPTED, REJECTED
    private long createdAt;
    private long respondedAt;

    public ProjectInvitation() {} // Firebase용 기본 생성자

    public ProjectInvitation(String invitationId, String projectId, String projectName,
                             String inviterUid, String inviterEmail, String inviteeEmail) {
        this.invitationId = invitationId;
        this.projectId = projectId;
        this.projectName = projectName;
        this.inviterUid = inviterUid;
        this.inviterEmail = inviterEmail;
        this.inviteeEmail = inviteeEmail;
        this.status = "PENDING";
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getInvitationId() { return invitationId; }
    public void setInvitationId(String invitationId) { this.invitationId = invitationId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getInviterUid() { return inviterUid; }
    public void setInviterUid(String inviterUid) { this.inviterUid = inviterUid; }

    public String getInviterEmail() { return inviterEmail; }
    public void setInviterEmail(String inviterEmail) { this.inviterEmail = inviterEmail; }

    public String getInviteeEmail() { return inviteeEmail; }
    public void setInviteeEmail(String inviteeEmail) { this.inviteeEmail = inviteeEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.respondedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getRespondedAt() { return respondedAt; }
    public void setRespondedAt(long respondedAt) { this.respondedAt = respondedAt; }
}