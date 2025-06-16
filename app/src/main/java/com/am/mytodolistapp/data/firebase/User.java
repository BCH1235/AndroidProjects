package com.am.mytodolistapp.data.firebase;

import java.io.Serializable;

// 'users' 컬렉션에 저장될 데이터 모델 클래스,가입한 사용자 한 명의 정보를 나타낸다.
//FirebaseRepository: 이 클래스의 객체를 사용하여 Firestore에 사용자 정보를 저장하거나, UID를 통해 사용자 정보를 조회
 // AuthFragment: 로그인 또는 회원가입 성공 시, Firebase Authentication에서 받은 사용자 정보를 이 User 객체로 만들어 Firestore에 저장
 // ProjectTaskListViewModel: 프로젝트 멤버들의 UID 목록을 이용해 각 멤버의 상세 정보를 가져와 UI에 표시할 때 사용

public class User implements Serializable {
    private String uid; //사용자의 고유 식별자
    private String email; // 사용자의 이메일 주소.
    private String displayName; // 사용자가 설정한 닉네임
    private long createdAt; // 사용자가 처음으로 앱에 가입한 시간

    public User() {} // Firebase용 기본 생성자

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}