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
import com.google.firebase.firestore.WriteBatch;

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

    // 로그아웃 처리
    public void signOut(OnCompleteListener<Void> listener) {
        try {
            // 모든 리스너 해제
            removeAllListeners();

            // Firebase Auth에서 로그아웃
            auth.signOut();

            Log.d(TAG, "User signed out successfully");
            if (listener != null) {
                listener.onSuccess(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during sign out", e);
            if (listener != null) {
                listener.onFailure(e);
            }
        }
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

    // 프로젝트 상세 정보 가져오기
    public void getProjectDetails(String projectId, OnCompleteListener<Project> listener) {
        db.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        Log.d(TAG, "Project details loaded successfully");
                        if (listener != null) listener.onSuccess(project);
                    } else {
                        Exception exception = new Exception("Project not found");
                        Log.e(TAG, "Project not found for ID: " + projectId);
                        if (listener != null) listener.onFailure(exception);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading project details", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 여러 사용자 정보를 개별적으로 가져오기 (권한 문제 해결)
    public void getUsersInfo(List<String> userIds, OnCompleteListener<List<User>> listener) {
        if (userIds == null || userIds.isEmpty()) {
            if (listener != null) listener.onSuccess(new ArrayList<>());
            return;
        }

        List<User> allUsers = new ArrayList<>();
        final int[] completedRequests = {0};
        final int totalRequests = userIds.size();

        // 각 사용자 문서를 개별적으로 읽어오기
        for (String userId : userIds) {
            db.collection(COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        synchronized (allUsers) {
                            if (documentSnapshot.exists()) {
                                User user = documentSnapshot.toObject(User.class);
                                if (user != null) {
                                    allUsers.add(user);
                                    Log.d(TAG, "User loaded: " + user.getEmail());
                                }
                            } else {
                                Log.w(TAG, "User document not found for ID: " + userId);
                                // 문서가 없는 경우 기본값으로 처리
                                User unknownUser = new User();
                                unknownUser.setUid(userId);
                                unknownUser.setEmail("알 수 없는 사용자");
                                unknownUser.setDisplayName("Unknown User");
                                allUsers.add(unknownUser);
                            }

                            completedRequests[0]++;

                            if (completedRequests[0] == totalRequests) {
                                Log.d(TAG, "All user info loaded successfully. Total users: " + allUsers.size());
                                if (listener != null) listener.onSuccess(allUsers);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading user info for ID: " + userId, e);
                        synchronized (allUsers) {
                            // 에러가 발생한 경우에도 기본값으로 처리하여 전체 로딩이 중단되지 않도록 함
                            User errorUser = new User();
                            errorUser.setUid(userId);
                            errorUser.setEmail("로딩 실패");
                            errorUser.setDisplayName("Load Failed");
                            allUsers.add(errorUser);

                            completedRequests[0]++;

                            if (completedRequests[0] == totalRequests) {
                                Log.d(TAG, "User info loading completed with some errors. Total users: " + allUsers.size());
                                if (listener != null) listener.onSuccess(allUsers);
                            }
                        }
                    });
        }
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

    // 프로젝트와 관련된 모든 할 일을 함께 삭제하는 메소드
    public void deleteProjectAndTasks(String projectId, OnCompleteListener<Void> listener) {
        Log.d(TAG, "Starting to delete project and tasks for projectId: " + projectId);

        // 1단계: 먼저 해당 프로젝트의 모든 할 일을 가져와서 삭제
        db.collection(COLLECTION_PROJECT_TASKS)
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // 할 일이 없는 경우 바로 프로젝트 삭제
                        Log.d(TAG, "No tasks found for project, proceeding to delete project");
                        deleteProjectDocument(projectId, listener);
                    } else {
                        // 할 일이 있는 경우 배치로 삭제
                        Log.d(TAG, "Found " + querySnapshot.size() + " tasks to delete");
                        deleteTasksInBatches(querySnapshot, projectId, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching tasks for deletion", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 할 일들을 배치로 삭제하는 메소드
    private void deleteTasksInBatches(com.google.firebase.firestore.QuerySnapshot querySnapshot,
                                      String projectId, OnCompleteListener<Void> listener) {
        WriteBatch batch = db.batch();
        int batchSize = 0;
        final int maxBatchSize = 500; // Firestore 배치 제한

        for (QueryDocumentSnapshot document : querySnapshot) {
            batch.delete(document.getReference());
            batchSize++;

            // 배치 크기가 500에 도달하면 실행
            if (batchSize >= maxBatchSize) {
                executeBatch(batch, projectId, listener);
                batch = db.batch();
                batchSize = 0;
            }
        }

        // 남은 배치 실행
        if (batchSize > 0) {
            executeBatch(batch, projectId, listener);
        } else {
            // 배치할 것이 없으면 바로 프로젝트 삭제
            deleteProjectDocument(projectId, listener);
        }
    }

    // 배치 실행 메소드
    private void executeBatch(WriteBatch batch, String projectId, OnCompleteListener<Void> listener) {
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Batch of tasks deleted successfully");
                    // 모든 할 일 삭제 완료 후 프로젝트 삭제
                    deleteProjectDocument(projectId, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting batch of tasks", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 프로젝트 문서 삭제 메소드
    private void deleteProjectDocument(String projectId, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Project deleted successfully: " + projectId);
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting project", e);
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

        // 사용자 인증 확인
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated for invitations");
            invitationsLiveData.setValue(new ArrayList<>());
            return invitationsLiveData;
        }

        Log.d(TAG, "Attempting to listen for invitations for email: " + userEmail);
        Log.d(TAG, "Current user: " + currentUser.getEmail());

        ListenerRegistration listener = db.collection(COLLECTION_INVITATIONS)
                .whereEqualTo("inviteeEmail", userEmail)
                .whereEqualTo("status", "PENDING")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to invitations", e);
                        Log.e(TAG, "Error code: " + e.getCode());
                        Log.e(TAG, "Error message: " + e.getMessage());

                        // 권한 에러인 경우 빈 목록 반환 (앱 크래시 방지)
                        invitationsLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    Log.d(TAG, "Successfully received invitation query result");
                    List<ProjectInvitation> invitations = new ArrayList<>();
                    if (querySnapshot != null) {
                        Log.d(TAG, "Query result size: " + querySnapshot.size());
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            try {
                                ProjectInvitation invitation = document.toObject(ProjectInvitation.class);
                                invitations.add(invitation);
                                Log.d(TAG, "Invitation loaded: " + invitation.getProjectName());
                            } catch (Exception parseError) {
                                Log.e(TAG, "Error parsing invitation document", parseError);
                            }
                        }
                    } else {
                        Log.d(TAG, "Query snapshot is null");
                    }

                    Log.d(TAG, "Total invitations loaded: " + invitations.size());
                    invitationsLiveData.setValue(invitations);
                });

        activeListeners.add(listener);
        return invitationsLiveData;
    }

    // 초대 응답 (수락/거절) - 수정된 버전
    public void respondToInvitation(String invitationId, String status, String projectId,
                                    String userId, OnCompleteListener<Void> listener) {

        // 1단계: 초대 상태 업데이트
        db.collection(COLLECTION_INVITATIONS)
                .document(invitationId)
                .update("status", status, "respondedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation status updated successfully");

                    if ("ACCEPTED".equals(status)) {
                        // 2단계: 수락한 경우 프로젝트에 멤버 추가
                        // 먼저 프로젝트 정보를 가져와서 소유자 정보 확인
                        addUserToProjectAfterAcceptance(projectId, userId, listener);
                    } else {
                        // 거절한 경우 완료
                        if (listener != null) listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating invitation status", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 초대 수락 후 프로젝트에 사용자 추가
    private void addUserToProjectAfterAcceptance(String projectId, String userId, OnCompleteListener<Void> listener) {
        DocumentReference projectRef = db.collection(COLLECTION_PROJECTS).document(projectId);

        projectRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null) {
                            List<String> memberIds = project.getMemberIds();
                            if (memberIds == null) {
                                memberIds = new ArrayList<>();
                            }

                            // 이미 멤버인지 확인
                            if (!memberIds.contains(userId)) {
                                memberIds.add(userId);
                                project.setMemberIds(memberIds);

                                Map<String, String> memberRoles = project.getMemberRoles();
                                if (memberRoles == null) {
                                    memberRoles = new HashMap<>();
                                }
                                memberRoles.put(userId, "member");
                                project.setMemberRoles(memberRoles);

                                // 프로젝트 업데이트 (이때는 시스템 권한으로 처리됨)
                                projectRef.set(project)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "User successfully added to project after invitation acceptance");
                                            if (listener != null) listener.onSuccess(null);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error adding user to project after acceptance", e);
                                            // 초대는 수락되었지만 프로젝트 멤버 추가 실패
                                            // 이 경우 사용자에게 알림 후 나중에 재시도하도록 할 수 있음
                                            if (listener != null) listener.onFailure(e);
                                        });
                            } else {
                                Log.d(TAG, "User is already a member of the project");
                                if (listener != null) listener.onSuccess(null);
                            }
                        } else {
                            Exception e = new Exception("Failed to parse project data");
                            Log.e(TAG, "Error parsing project data", e);
                            if (listener != null) listener.onFailure(e);
                        }
                    } else {
                        Exception e = new Exception("Project not found");
                        Log.e(TAG, "Project not found for ID: " + projectId, e);
                        if (listener != null) listener.onFailure(e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error reading project for member addition", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 프로젝트에 멤버 추가
    private void addMemberToProject(String projectId, String userId, OnCompleteListener<Void> listener) {
        DocumentReference projectRef = db.collection(COLLECTION_PROJECTS).document(projectId);

        // 현재 사용자가 프로젝트 소유자인지 확인 후 처리
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            if (listener != null) {
                listener.onFailure(new Exception("User not authenticated"));
            }
            return;
        }

        // 프로젝트 정보를 먼저 읽어온다
        projectRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null) {
                            // 현재 사용자가 프로젝트 소유자인지 확인
                            if (!currentUser.getUid().equals(project.getOwnerId())) {
                                Log.e(TAG, "Only project owner can add members");
                                if (listener != null) {
                                    listener.onFailure(new Exception("Only project owner can add members"));
                                }
                                return;
                            }

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

                                // 업데이트된 프로젝트 저장
                                projectRef.set(project)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Member added to project successfully");
                                            if (listener != null) listener.onSuccess(null);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error updating project with new member", e);
                                            if (listener != null) listener.onFailure(e);
                                        });
                            } else {
                                Log.d(TAG, "User is already a member");
                                if (listener != null) listener.onSuccess(null);
                            }
                        } else {
                            Exception e = new Exception("Failed to parse project data");
                            Log.e(TAG, "Error parsing project", e);
                            if (listener != null) listener.onFailure(e);
                        }
                    } else {
                        Exception e = new Exception("Project not found");
                        Log.e(TAG, "Project not found", e);
                        if (listener != null) listener.onFailure(e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error reading project", e);
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