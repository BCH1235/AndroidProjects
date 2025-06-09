// FirebaseCollaborationService.java
// 위치: app/src/main/java/com/am/mytodolistapp/service/FirebaseCollaborationService.java
package com.am.mytodolistapp.service;

import android.util.Log;

import com.am.mytodolistapp.data.CollaborationProject;
import com.am.mytodolistapp.data.CollaborationTodoItem;
import com.am.mytodolistapp.data.ProjectMember;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class FirebaseCollaborationService {

    private static final String TAG = "FirebaseCollaboration";
    private static final String COLLECTION_PROJECTS = "collaboration_projects";
    private static final String COLLECTION_TODOS = "collaboration_todos";
    private static final String COLLECTION_MEMBERS = "project_members";
    private static final String COLLECTION_INVITATIONS = "invitations";

    private FirebaseFirestore db;

    public FirebaseCollaborationService() {
        db = FirebaseFirestore.getInstance();
    }

    // ========== 프로젝트 관련 메서드 ==========

    public void createProject(CollaborationProject project, ProjectMember owner, ProjectCallback callback) {
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("projectId", project.getProjectId());
        projectData.put("name", project.getName());
        projectData.put("description", project.getDescription());
        projectData.put("ownerId", project.getOwnerId());
        projectData.put("ownerName", project.getOwnerName());
        projectData.put("createdAt", project.getCreatedAt());
        projectData.put("updatedAt", project.getUpdatedAt());
        projectData.put("isActive", project.isActive());
        projectData.put("memberCount", 1);

        db.collection(COLLECTION_PROJECTS)
                .document(project.getProjectId())
                .set(projectData)
                .addOnSuccessListener(aVoid -> {
                    // 프로젝트 생성 성공 후 소유자를 멤버로 추가
                    addProjectMember(owner, new MemberCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "프로젝트 생성 완료: " + project.getName());
                            callback.onSuccess();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "프로젝트 멤버 추가 실패: " + error);
                            callback.onError(error);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "프로젝트 생성 실패", e);
                    callback.onError(e.getMessage());
                });
    }

    public void updateProject(CollaborationProject project) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", project.getName());
        updates.put("description", project.getDescription());
        updates.put("updatedAt", System.currentTimeMillis());
        updates.put("memberCount", project.getMemberCount());

        db.collection(COLLECTION_PROJECTS)
                .document(project.getProjectId())
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "프로젝트 업데이트 성공"))
                .addOnFailureListener(e -> Log.e(TAG, "프로젝트 업데이트 실패", e));
    }

    public ListenerRegistration listenToProjectChanges(String userId, ProjectChangesListener listener) {
        return db.collection(COLLECTION_MEMBERS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .whereEqualTo("invitationStatus", "accepted")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "프로젝트 변경 리스너 오류", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        listener.onProjectsChanged();
                    }
                });
    }

    // ========== 멤버 관련 메서드 ==========

    public void inviteMember(String projectId, ProjectMember invitation, InviteCallback callback) {
        // 초대장 생성
        Map<String, Object> invitationData = new HashMap<>();
        invitationData.put("projectId", projectId);
        invitationData.put("userId", invitation.getUserId());
        invitationData.put("userName", invitation.getUserName());
        invitationData.put("userEmail", invitation.getUserEmail());
        invitationData.put("role", invitation.getRole());
        invitationData.put("invitationStatus", "pending");
        invitationData.put("createdAt", System.currentTimeMillis());

        String invitationId = projectId + "_" + invitation.getUserId();

        db.collection(COLLECTION_INVITATIONS)
                .document(invitationId)
                .set(invitationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "초대 전송 완료: " + invitation.getUserEmail());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "초대 전송 실패", e);
                    callback.onError(e.getMessage());
                });
    }

    public void acceptInvitation(ProjectMember member, AcceptCallback callback) {
        // 멤버로 추가
        addProjectMember(member, new MemberCallback() {
            @Override
            public void onSuccess() {
                // 초대장 상태 업데이트
                String invitationId = member.getProjectId() + "_" + member.getUserId();
                db.collection(COLLECTION_INVITATIONS)
                        .document(invitationId)
                        .update("invitationStatus", "accepted")
                        .addOnSuccessListener(aVoid -> {
                            // 프로젝트 멤버 수 증가
                            updateProjectMemberCount(member.getProjectId());
                            Log.d(TAG, "초대 수락 완료");
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "초대 상태 업데이트 실패", e);
                            callback.onError(e.getMessage());
                        });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void declineInvitation(ProjectMember member) {
        String invitationId = member.getProjectId() + "_" + member.getUserId();
        db.collection(COLLECTION_INVITATIONS)
                .document(invitationId)
                .update("invitationStatus", "declined")
                .addOnSuccessListener(aVoid -> Log.d(TAG, "초대 거절 완료"))
                .addOnFailureListener(e -> Log.e(TAG, "초대 거절 실패", e));
    }

    private void addProjectMember(ProjectMember member, MemberCallback callback) {
        Map<String, Object> memberData = new HashMap<>();
        memberData.put("projectId", member.getProjectId());
        memberData.put("userId", member.getUserId());
        memberData.put("userName", member.getUserName());
        memberData.put("userEmail", member.getUserEmail());
        memberData.put("role", member.getRole());
        memberData.put("joinedAt", member.getJoinedAt());
        memberData.put("isActive", true);
        memberData.put("invitationStatus", member.getInvitationStatus());

        String memberId = member.getProjectId() + "_" + member.getUserId();

        db.collection(COLLECTION_MEMBERS)
                .document(memberId)
                .set(memberData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "멤버 추가 완료: " + member.getUserName());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "멤버 추가 실패", e);
                    callback.onError(e.getMessage());
                });
    }

    private void updateProjectMemberCount(String projectId) {
        db.collection(COLLECTION_MEMBERS)
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("isActive", true)
                .whereEqualTo("invitationStatus", "accepted")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int memberCount = queryDocumentSnapshots.size();
                    db.collection(COLLECTION_PROJECTS)
                            .document(projectId)
                            .update("memberCount", memberCount)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "멤버 수 업데이트: " + memberCount))
                            .addOnFailureListener(e -> Log.e(TAG, "멤버 수 업데이트 실패", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "멤버 수 조회 실패", e));
    }

    // ========== 할일 관련 메서드 ==========

    public void createTodo(CollaborationTodoItem todo, TodoCallback callback) {
        Map<String, Object> todoData = new HashMap<>();
        todoData.put("todoId", todo.getTodoId());
        todoData.put("projectId", todo.getProjectId());
        todoData.put("title", todo.getTitle());
        todoData.put("content", todo.getContent());
        todoData.put("isCompleted", todo.isCompleted());
        todoData.put("createdById", todo.getCreatedById());
        todoData.put("createdByName", todo.getCreatedByName());
        todoData.put("assignedToId", todo.getAssignedToId());
        todoData.put("assignedToName", todo.getAssignedToName());
        todoData.put("priority", todo.getPriority());
        todoData.put("dueDate", todo.getDueDate());
        todoData.put("createdAt", todo.getCreatedAt());
        todoData.put("updatedAt", todo.getUpdatedAt());

        db.collection(COLLECTION_TODOS)
                .document(todo.getTodoId())
                .set(todoData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "할일 생성 완료: " + todo.getTitle());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "할일 생성 실패", e);
                    callback.onError(e.getMessage());
                });
    }

    public void updateTodo(CollaborationTodoItem todo) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", todo.getTitle());
        updates.put("content", todo.getContent());
        updates.put("isCompleted", todo.isCompleted());
        updates.put("assignedToId", todo.getAssignedToId());
        updates.put("assignedToName", todo.getAssignedToName());
        updates.put("priority", todo.getPriority());
        updates.put("dueDate", todo.getDueDate());
        updates.put("updatedAt", System.currentTimeMillis());
        updates.put("completedAt", todo.getCompletedAt());
        updates.put("completedById", todo.getCompletedById());
        updates.put("completedByName", todo.getCompletedByName());

        db.collection(COLLECTION_TODOS)
                .document(todo.getTodoId())
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "할일 업데이트 성공"))
                .addOnFailureListener(e -> Log.e(TAG, "할일 업데이트 실패", e));
    }

    public void deleteTodo(String todoId) {
        db.collection(COLLECTION_TODOS)
                .document(todoId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "할일 삭제 완료"))
                .addOnFailureListener(e -> Log.e(TAG, "할일 삭제 실패", e));
    }

    public ListenerRegistration listenToProjectTodos(String projectId, TodoChangesListener listener) {
        return db.collection(COLLECTION_TODOS)
                .whereEqualTo("projectId", projectId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "할일 변경 리스너 오류", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        listener.onTodosChanged();
                    }
                });
    }

    // ========== 콜백 인터페이스들 ==========

    public interface ProjectCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface InviteCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface AcceptCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface MemberCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface TodoCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface ProjectChangesListener {
        void onProjectsChanged();
    }

    public interface TodoChangesListener {
        void onTodosChanged();
    }
}