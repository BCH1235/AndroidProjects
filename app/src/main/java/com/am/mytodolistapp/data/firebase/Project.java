package com.am.mytodolistapp.data.firebase;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

// Firestore의 'projects' 컬렉션에 저장될 데이터 모델 클래스
// 이 클래스는 하나의 협업 프로젝트 정보를 나타낸다.
// FirebaseRepository: 이 클래스의 객체를 사용하여 Firestore와 데이터를 주고받는다
// CollaborationViewModel: UI에 표시할 프로젝트 데이터를 관리하기 위해 이 클래스를 사용
//  순수 데이터만 담는 그릇 역할, 복잡한 로직 없이 데이터와 getter/setter 메서드로 구성
public class Project implements Serializable {
    private String projectId; //  Firestore 문서의 고유 ID. 프로젝트를 식별하는 기본 키
    private String projectName;// 프로젝트의 이름
    private String description; // 프로젝트에 대한 상세 설명.
    private String ownerId;  // 프로젝트를 생성한 사용자의 UID
    private List<String> memberIds; // 참여 멤버 UID 목록
    private Map<String, String> memberRoles; // 멤버별 역할 (owner, member 등)
    private long createdAt; // 프로젝트가 생성된 시간
    private long updatedAt; // 프로젝트 정보가 마지막으로 수정된 시간

    public Project() {} // Firebase용 기본 생성자

    public Project(String projectId, String projectName, String description, String ownerId) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = System.currentTimeMillis(); // 생성 시각을 현재 시간으로 초기화
        this.updatedAt = System.currentTimeMillis(); // 수정 시각도 생성 시각과 동일하게 초기화
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