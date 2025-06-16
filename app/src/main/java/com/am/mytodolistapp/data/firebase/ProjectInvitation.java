package com.am.mytodolistapp.data.firebase;


// 'invitations' 컬렉션에 저장될 데이터 모델 클래스
// 순수 데이터 객체
// FirebaseRepository: 이 클래스의 객체를 사용하여 Firestore에 초대 정보를 저장하거나, 받은 초대 목록을 조회
// CollaborationViewModel: 사용자에게 받은 초대 목록을 보여주거나, 새로운 초대를 보내는 로직을 처리할 때 이 클래스를 사용
public class ProjectInvitation {
    private String invitationId; // 초대 문서의 고유 ID.
    private String projectId; // 초대가 이루어진 프로젝트의 ID.
    private String projectName; // 초대된 프로젝트의 이름
    private String inviterUid; // 초대를 보낸 사람의 UID.
    private String inviterEmail; // 초대를 보낸 사람의 이메일
    private String inviteeEmail; // 초대를 받은 사람의 이메일
    private String status; // PENDING(대기중), ACCEPTED(수락됨), REJECTED (거절됨)
    private long createdAt; //초대가 생성된 시간
    private long respondedAt; // 초대에 응답한 시간

    public ProjectInvitation() {} // Firebase용 기본 생성자

    public ProjectInvitation(String invitationId, String projectId, String projectName,
                             String inviterUid, String inviterEmail, String inviteeEmail) {
        this.invitationId = invitationId;
        this.projectId = projectId;
        this.projectName = projectName;
        this.inviterUid = inviterUid;
        this.inviterEmail = inviterEmail;
        this.inviteeEmail = inviteeEmail;
        this.status = "PENDING"; //초대 생성 시 상태는 항상 '대기중'으로 초기화
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