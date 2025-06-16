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



// Firebase와의 모든 데이터 통신을 담당하는 Repository 클래스
// Firebase Authentication(인증), Firestore(데이터베이스) 관련 모든 CRUD(생성, 읽기, 수정, 삭제) 작업을 처리
// 연관 클래스: CollaborationViewModel, ProjectTaskListViewModel 등 ViewModel에서 이 클래스를 사용하여 데이터를 요청한다.
// 싱글톤 패턴
public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_PROJECTS = "projects";
    private static final String COLLECTION_PROJECT_TASKS = "project_tasks";
    private static final String COLLECTION_INVITATIONS = "invitations";

    private FirebaseFirestore db; // Firestore 데이터베이스 인스턴스
    private FirebaseAuth auth; // Firebase 인증 인스턴스
    private static FirebaseRepository instance; // 싱글톤 인스턴스

    private List<ListenerRegistration> activeListeners = new ArrayList<>(); // Firestore 리스너들을 관리하는 리스트

    // FirebaseRepository의 싱글톤 인스턴스를 반환
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
     //private 생성자. 외부에서 직접 인스턴스를 생성하는 것을 막는다.
     //이 클래스가 처음 생성될 때 Firebase 서비스들을 초기화



    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }// 현재 로그인된 Firebase 사용자 객체를 반환

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }// 사용자의 로그인 상태를 확인

    public void signOut(OnCompleteListener<Void> listener) {
        try {
            removeAllListeners(); //리스너를 제거해야  백그라운드에서 계속 동작할때 리소스 낭비를 줄일 수 있음
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
    }//사용자 로그아웃 처리

    public void saveUser(User user, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_USERS)
                .document(user.getUid()) // 사용자의 고유 ID를 문서 ID로 사용
                .set(user) // User 객체를 Firestore 문서로 저장
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved successfully");
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user", e);
                    if (listener != null) listener.onFailure(e);
                });
    }// Firestore의 'users' 컬렉션에 저장하거나 업데이트,회원가입 또는 구글 로그인 성공 후 `AuthFragment`에서 호출된다.

    public void createProject(Project project, OnCompleteListener<String> listener) { // 새로운 협업 프로젝트를 생성
        // ID를 먼저 생성
        DocumentReference projectRef = db.collection(COLLECTION_PROJECTS).document();
        project.setProjectId(projectRef.getId());

        // 생성자를 멤버 목록과 역할 맵에 추가
        List<String> memberIds = new ArrayList<>();
        memberIds.add(project.getOwnerId());
        project.setMemberIds(memberIds);

        Map<String, String> memberRoles = new HashMap<>();
        memberRoles.put(project.getOwnerId(), "owner");
        project.setMemberRoles(memberRoles);

        project.setUpdatedAt(System.currentTimeMillis());

        // Firestore에 저장
        projectRef.set(project)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Project created successfully with ID: " + project.getProjectId());
                    if (listener != null) listener.onSuccess(project.getProjectId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating project", e);
                    if (listener != null) listener.onFailure(e);
                });
    } // `CollaborationViewModel`에서 사용자가 프로젝트 생성을 요청할 때 호출된다.

    public LiveData<List<Project>> getUserProjects(String userId) {
        MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();
        // 실시간 업데이트를 위한 리스너 등록
        ListenerRegistration listener = db.collection(COLLECTION_PROJECTS)
                .whereArrayContains("memberIds", userId) // 'memberIds' 배열에 userId가 포함된 문서를 찾음
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
                            Log.d(TAG, "Project loaded: " + project.getProjectName());
                        }
                    }
                    Log.d(TAG, "Total projects loaded: " + projects.size());
                    projectsLiveData.postValue(projects); // LiveData 값 업데이트
                });

        activeListeners.add(listener);
        return projectsLiveData;
    } //특정 사용자가 참여하고 있는 모든 프로젝트 목록을 실시간으로 가져온다.`CollaborationViewModel`에서 이 LiveData를 관찰하여 UI에 프로젝트 목록을 표시

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
    }// 특정 프로젝트의 상세 정보.`ProjectTaskListViewModel`에서 멤버 목록을 가져오기 전에 프로젝트 정보를 확인할 때 사용

    public void getUsersInfo(List<String> userIds, OnCompleteListener<List<User>> listener) {
        if (userIds == null || userIds.isEmpty()) {
            if (listener != null) listener.onSuccess(new ArrayList<>());
            return;
        }

        List<User> allUsers = new ArrayList<>();
        final int[] completedRequests = {0};
        final int totalRequests = userIds.size();

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
                            } else { // 사용자가 탈퇴했거나 DB에 정보가 없는 경우
                                Log.w(TAG, "User document not found for ID: " + userId);
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
    }// 사용자의 UID 목록을 받아 각 사용자의 상세 정보를 가져온다.`ProjectTaskListViewModel`에서 프로젝트 멤버 목록을 화면에 표시할 때 사용

    public LiveData<List<ProjectTask>> getProjectTasks(String projectId) {
        MutableLiveData<List<ProjectTask>> tasksLiveData = new MutableLiveData<>();

        ListenerRegistration listener = db.collection(COLLECTION_PROJECT_TASKS)
                .whereEqualTo("projectId", projectId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {  //`addSnapshotListener`를 사용하여 실시간 변경을 감지하고 `LiveData`로 반환
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
    } //특정 프로젝트의 할 일 목록을 실시간으로 가져온다.`ProjectTaskListViewModel`에서 이 LiveData를 관찰하여 프로젝트 할 일 목록 UI를 업데이트

    public void addProjectTask(ProjectTask task, OnCompleteListener<String> listener) {
        DocumentReference taskRef = db.collection(COLLECTION_PROJECT_TASKS).document();
        task.setTaskId(taskRef.getId()); // 먼저 문서 ID를 생성하여 Task 객체에 설정한 후 Firestore에 저장

        taskRef.set(task)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task added successfully");
                    if (listener != null) listener.onSuccess(task.getTaskId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding task", e);
                    if (listener != null) listener.onFailure(e);
                });
    }// 프로젝트에 새로운 할 일을 추가

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
    } //기존 프로젝트 할 일 정보를 업데이트

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
    } // 프로젝트 할 일을 삭제

    public void deleteProjectAndTasks(String projectId, OnCompleteListener<Void> listener) {
        Log.d(TAG, "Starting to delete project and tasks for projectId: " + projectId);

        db.collection(COLLECTION_PROJECT_TASKS)
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) { // 삭제할 할 일이 없으면 바로 프로젝트 문서 삭제
                        Log.d(TAG, "No tasks found for project, proceeding to delete project");
                        deleteProjectDocument(projectId, listener);
                    } else { // 할 일이 있으면 배치 삭제 실행
                        Log.d(TAG, "Found " + querySnapshot.size() + " tasks to delete");
                        deleteTasksInBatches(querySnapshot, projectId, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching tasks for deletion", e);
                    if (listener != null) listener.onFailure(e);
                });
    } //프로젝트와 그에 속한 모든 할 일을 삭제.먼저 프로젝트에 속한 모든 할 일을 조회한 후 조회된 할 일들을 `WriteBatch`를 사용하여 한 번에 삭제 요청하고 모든 할 일 삭제가 성공하면, 마지막으로 프로젝트 문서를 삭제한다.
    // `CollaborationViewModel`에서 프로젝트 삭제 요청 시 호출

    private void deleteTasksInBatches(com.google.firebase.firestore.QuerySnapshot querySnapshot,
                                      String projectId, OnCompleteListener<Void> listener) {
        WriteBatch batch = db.batch();
        int batchSize = 0;
        final int maxBatchSize = 500;

        for (QueryDocumentSnapshot document : querySnapshot) {
            batch.delete(document.getReference());
            batchSize++;

            if (batchSize >= maxBatchSize) {
                executeBatch(batch, projectId, listener);
                batch = db.batch();
                batchSize = 0;
            }
        }

        if (batchSize > 0) {
            executeBatch(batch, projectId, listener);
        } else {
            deleteProjectDocument(projectId, listener);
        }
    }// 여러 문서를 배치(바구니)로 삭제,단위로 묶어서 삭제

    private void executeBatch(WriteBatch batch, String projectId, OnCompleteListener<Void> listener) {
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Batch of tasks deleted successfully");
                    deleteProjectDocument(projectId, listener); //배치 삭제 성공 후 프로젝트 문서 삭제
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting batch of tasks", e);
                    if (listener != null) listener.onFailure(e);
                });
    } //WriteBatch를 실행하고, 성공 시 프로젝트 문서를 삭제하는 다음 단계로 넘어간다.

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
    } // 프로젝트 문서 자체를 삭제합니다.

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
    } // 다른 사용자에게 프로젝트 초대.'invitations' 컬렉션에 초대 정보를 담은 새 문서를 생성

    public LiveData<List<ProjectInvitation>> getUserInvitations(String userEmail) {
        MutableLiveData<List<ProjectInvitation>> invitationsLiveData = new MutableLiveData<>();

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
                .whereEqualTo("status", "PENDING") // 아직 응답하지 않은 초대만
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to invitations", e);
                        Log.e(TAG, "Error code: " + e.getCode());
                        Log.e(TAG, "Error message: " + e.getMessage());

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
    } // 현재 사용자가 받은 초대 목록을 실시간으로 가져온다.'invitations' 컬렉션에서 `inviteeEmail`이 현재 사용자 이메일과 일치하고, `status`가 'PENDING(대기중)'인 문서를 실시간으로 감지

    public void respondToInvitation(String invitationId, String status, String projectId,
                                    String userId, OnCompleteListener<Void> listener) {

        db.collection(COLLECTION_INVITATIONS)
                .document(invitationId)
                .update("status", status, "respondedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation status updated successfully");

                    if ("ACCEPTED".equals(status)) {
                        addUserToProjectAfterAcceptance(projectId, userId, listener); //만약 수락했다면, 메소드를 호출하여 해당 프로젝트에 사용자를 멤버로 추가
                    } else {
                        if (listener != null) listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating invitation status", e);
                    if (listener != null) listener.onFailure(e);
                });
    } // 받은 초대에 대해 수락 또는 거절 응답을 처리.'invitations' 문서의 `status` 필드를 'ACCEPTED' 또는 'REJECTED'로 업데이트

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

                            if (!memberIds.contains(userId)) {
                                memberIds.add(userId);
                                project.setMemberIds(memberIds);

                                Map<String, String> memberRoles = project.getMemberRoles();
                                if (memberRoles == null) {
                                    memberRoles = new HashMap<>();
                                }
                                memberRoles.put(userId, "member");
                                project.setMemberRoles(memberRoles);

                                projectRef.set(project)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "User successfully added to project after invitation acceptance");
                                            if (listener != null) listener.onSuccess(null);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error adding user to project after acceptance", e);
                                            if (listener != null) listener.onFailure(e);
                                        });
                            } else {
                                Log.d(TAG, "User is already a member of the project");
                                if (listener != null) listener.onSuccess(null); // 이미 멤버인 경우
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
    } //초대 수락 후, 사용자를 프로젝트 멤버 목록에 추가.프로젝트 문서를 가져온 후 `memberIds` 배열과 `memberRoles` 맵에 새로운 사용자의 정보를 추가한다.업데이트된 프로젝트 문서를 다시 저장

    private void addMemberToProject(String projectId, String userId, OnCompleteListener<Void> listener) {
        DocumentReference projectRef = db.collection(COLLECTION_PROJECTS).document(projectId);

        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            if (listener != null) {
                listener.onFailure(new Exception("User not authenticated"));
            }
            return;
        }

        projectRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null) {
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

    public void removeAllListeners() {
        for (ListenerRegistration listener : activeListeners) {
            listener.remove();
        }
        activeListeners.clear();
    } //모든 활성 Firestore 리스너를 제거.메모리 누수와 불필요한 데이터 수신을 방지하기 위해 필수다.

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}