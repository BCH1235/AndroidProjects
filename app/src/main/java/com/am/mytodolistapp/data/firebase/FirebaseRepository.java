package com.am.mytodolistapp.data.firebase;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";

    // Firestore 컬렉션 이름들
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_PROJECTS = "projects";
    private static final String COLLECTION_PROJECT_TASKS = "project_tasks";
    private static final String COLLECTION_INVITATIONS = "invitations";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static FirebaseRepository instance;

    // 실시간 리스너들
    private List<ListenerRegistration> activeListeners = new ArrayList<>();

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // 현재 사용자 정보 가져오기
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    // 사용자 정보를 Firestore에 저장
    public void saveUser(User user, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved successfully");
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    public void createProject(Project project, OnCompleteListener<String> listener) {
        DocumentReference projectRef = db.collection(COLLECTION_PROJECTS).document();
        project.setProjectId(projectRef.getId());

        // 생성자를 멤버로 추가
        List<String> memberIds = new ArrayList<>();
        memberIds.add(project.getOwnerId());
        project.setMemberIds(memberIds);

        // 생성자를 owner로 설정
        Map<String, String> memberRoles = new HashMap<>();
        memberRoles.put(project.getOwnerId(), "owner");
        project.setMemberRoles(memberRoles);

        // updatedAt 필드 설정 추가
        project.setUpdatedAt(System.currentTimeMillis());

        projectRef.set(project)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Project created successfully with ID: " + project.getProjectId());
                    if (listener != null) listener.onSuccess(project.getProjectId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating project", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    public LiveData<List<Project>> getUserProjects(String userId) {
        MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();

        ListenerRegistration listener = db.collection(COLLECTION_PROJECTS)
                .whereArrayContains("memberIds", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to projects", e);
                        return;
                    }

                    List<Project> projects = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Project project = document.toObject(Project.class);
                            projects.add(project);
                            Log.d(TAG, "Project loaded: " + project.getProjectName()); // 개별 프로젝트 로그
                        }
                    }
                    Log.d(TAG, "Total projects loaded: " + projects.size());
                    // UI 스레드에서 업데이트 보장
                    projectsLiveData.postValue(projects);
                });

        activeListeners.add(listener);
        return projectsLiveData;
    }

    // 프로젝트의 할 일 목록 가져오기 (실시간)
    public LiveData<List<ProjectTask>> getProjectTasks(String projectId) {
        MutableLiveData<List<ProjectTask>> tasksLiveData = new MutableLiveData<>();

        ListenerRegistration listener = db.collection(COLLECTION_PROJECT_TASKS)
                .whereEqualTo("projectId", projectId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to project tasks", e);
                        return;
                    }

                    List<ProjectTask> tasks = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            ProjectTask task = document.toObject(ProjectTask.class);
                            tasks.add(task);
                        }
                    }
                    tasksLiveData.setValue(tasks);
                });

        activeListeners.add(listener);
        return tasksLiveData;
    }

    // 할 일 추가
    public void addProjectTask(ProjectTask task, OnCompleteListener<String> listener) {
        DocumentReference taskRef = db.collection(COLLECTION_PROJECT_TASKS).document();
        task.setTaskId(taskRef.getId());

        taskRef.set(task)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task added successfully");
                    if (listener != null) listener.onSuccess(task.getTaskId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding task", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 할 일 업데이트
    public void updateProjectTask(ProjectTask task, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_PROJECT_TASKS)
                .document(task.getTaskId())
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task updated successfully");
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 할 일 삭제
    public void deleteProjectTask(String taskId, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_PROJECT_TASKS)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted successfully");
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting task", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 프로젝트 초대 보내기
    public void sendProjectInvitation(ProjectInvitation invitation, OnCompleteListener<String> listener) {
        DocumentReference invitationRef = db.collection(COLLECTION_INVITATIONS).document();
        invitation.setInvitationId(invitationRef.getId());

        invitationRef.set(invitation)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation sent successfully");
                    if (listener != null) listener.onSuccess(invitation.getInvitationId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending invitation", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 사용자의 초대 목록 가져오기 (실시간)
    public LiveData<List<ProjectInvitation>> getUserInvitations(String userEmail) {
        MutableLiveData<List<ProjectInvitation>> invitationsLiveData = new MutableLiveData<>();

        ListenerRegistration listener = db.collection(COLLECTION_INVITATIONS)
                .whereEqualTo("inviteeEmail", userEmail)
                .whereEqualTo("status", "PENDING")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to invitations", e);
                        return;
                    }

                    List<ProjectInvitation> invitations = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            ProjectInvitation invitation = document.toObject(ProjectInvitation.class);
                            invitations.add(invitation);
                        }
                    }
                    invitationsLiveData.setValue(invitations);
                });

        activeListeners.add(listener);
        return invitationsLiveData;
    }

    // 초대 응답 (수락/거절)
    public void respondToInvitation(String invitationId, String status, String projectId,
                                    String userId, OnCompleteListener<Void> listener) {
        // 초대 상태 업데이트
        db.collection(COLLECTION_INVITATIONS)
                .document(invitationId)
                .update("status", status, "respondedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if ("ACCEPTED".equals(status)) {
                        // 프로젝트에 멤버 추가
                        addMemberToProject(projectId, userId, listener);
                    } else {
                        if (listener != null) listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error responding to invitation", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 프로젝트에 멤버 추가
    private void addMemberToProject(String projectId, String userId, OnCompleteListener<Void> listener) {
        DocumentReference projectRef = db.collection(COLLECTION_PROJECTS).document(projectId);

        db.runTransaction(transaction -> {
            Project project = transaction.get(projectRef).toObject(Project.class);
            if (project != null) {
                List<String> memberIds = project.getMemberIds();
                if (memberIds == null) {
                    memberIds = new ArrayList<>();
                }
                if (!memberIds.contains(userId)) {
                    memberIds.add(userId);
                    project.setMemberIds(memberIds);

                    Map<String, String> memberRoles = project.getMemberRoles();
                    if (memberRoles == null) {
                        memberRoles = new HashMap<>();
                    }
                    memberRoles.put(userId, "member");
                    project.setMemberRoles(memberRoles);

                    transaction.set(projectRef, project);
                }
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Member added to project successfully");
            if (listener != null) listener.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error adding member to project", e);
            if (listener != null) listener.onFailure(e);
        });
    }

    // 모든 리스너 해제
    public void removeAllListeners() {
        for (ListenerRegistration listener : activeListeners) {
            listener.remove();
        }
        activeListeners.clear();
    }

    // 콜백 인터페이스
    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}