package com.am.mytodolistapp.ui.collaboration;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.am.mytodolistapp.data.TodoRepository;
import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.am.mytodolistapp.data.firebase.Project;
import com.am.mytodolistapp.data.firebase.ProjectTask;
import com.am.mytodolistapp.data.firebase.User;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ProjectTaskListViewModel extends AndroidViewModel {
    private static final String TAG = "ProjectTaskListVM";

    private FirebaseRepository firebaseRepository;
    private TodoRepository todoRepository; // 🆕 로컬 DB 연동
    private String currentProjectId;
    private LiveData<List<ProjectTask>> projectTasks;
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> successMessage = new MutableLiveData<>();
    private MutableLiveData<List<User>> projectMembers = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoadingMembers = new MutableLiveData<>();
    private MutableLiveData<Project> currentProject = new MutableLiveData<>();

    public ProjectTaskListViewModel(@NonNull Application application) {
        super(application);
        firebaseRepository = FirebaseRepository.getInstance();
        todoRepository = new TodoRepository(application); // 🆕 로컬 DB 연동

        Log.d(TAG, "ProjectTaskListViewModel initialized with sync integration");
    }

    public void setProjectId(String projectId) {
        this.currentProjectId = projectId;
        loadProjectTasks();

        Log.d(TAG, "Project ID set: " + projectId);
    }

    private void loadProjectTasks() {
        if (currentProjectId != null) {
            projectTasks = firebaseRepository.getProjectTasks(currentProjectId);
            Log.d(TAG, "Loading project tasks for: " + currentProjectId);
        }
    }

    public LiveData<List<ProjectTask>> getProjectTasks() {
        return projectTasks;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<List<User>> getProjectMembers() {
        return projectMembers;
    }

    public LiveData<Boolean> getIsLoadingMembers() {
        return isLoadingMembers;
    }

    public LiveData<Project> getCurrentProject() {
        return currentProject;
    }

    // 메시지를 리셋하는 메서드들
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    public void clearSuccessMessage() {
        successMessage.setValue(null);
    }

    // 프로젝트 멤버 목록 로드
    public void loadProjectMembers() {
        if (currentProjectId == null) {
            errorMessage.setValue("프로젝트가 선택되지 않았습니다.");
            return;
        }

        isLoadingMembers.setValue(true);
        Log.d(TAG, "Loading project members for project: " + currentProjectId);

        // 1단계: 프로젝트 상세 정보 가져오기
        firebaseRepository.getProjectDetails(currentProjectId, new FirebaseRepository.OnCompleteListener<Project>() {
            @Override
            public void onSuccess(Project project) {
                if (project != null && project.getMemberIds() != null && !project.getMemberIds().isEmpty()) {
                    Log.d(TAG, "Project loaded, member count: " + project.getMemberIds().size());

                    // 프로젝트 정보 저장
                    currentProject.setValue(project);

                    // 2단계: 멤버 ID들로 사용자 정보 가져오기
                    firebaseRepository.getUsersInfo(project.getMemberIds(), new FirebaseRepository.OnCompleteListener<List<User>>() {
                        @Override
                        public void onSuccess(List<User> users) {
                            Log.d(TAG, "Members loaded successfully: " + users.size());
                            projectMembers.setValue(users);
                            isLoadingMembers.setValue(false);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Failed to load member details", e);
                            errorMessage.setValue("멤버 정보를 불러오는데 실패했습니다: " + e.getMessage());
                            isLoadingMembers.setValue(false);
                        }
                    });
                } else {
                    Log.d(TAG, "No members found in project");
                    projectMembers.setValue(null);
                    isLoadingMembers.setValue(false);
                    errorMessage.setValue("프로젝트에 멤버가 없습니다.");
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load project details", e);
                errorMessage.setValue("프로젝트 정보를 불러오는데 실패했습니다: " + e.getMessage());
                isLoadingMembers.setValue(false);
            }
        });
    }

    public void addTask(String title, String content, Long dueDate) {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            errorMessage.setValue("사용자가 로그인되어 있지 않습니다.");
            return;
        }

        if (currentProjectId == null) {
            errorMessage.setValue("프로젝트가 선택되지 않았습니다.");
            return;
        }

        Log.d(TAG, "Adding new task: " + title + " to project: " + currentProjectId);

        ProjectTask task = new ProjectTask(null, currentProjectId, title, currentUser.getUid());
        task.setContent(content);
        if (dueDate != null) {
            task.setDueDate(dueDate);
        }

        firebaseRepository.addProjectTask(task, new FirebaseRepository.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String taskId) {
                successMessage.setValue("할 일이 추가되었습니다.");
                Log.d(TAG, "Task added with ID: " + taskId);

                // 🆕 성공 시 즉시 로컬 동기화 트리거 (선택사항)
                // 일반적으로는 CollaborationSyncService가 자동으로 감지하여 동기화하지만,
                // 즉시 반영을 원한다면 수동 동기화를 호출할 수 있습니다.
                // todoRepository.performManualSync();
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("할 일 추가에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to add task", e);
            }
        });
    }

    public void updateTask(ProjectTask task) {
        Log.d(TAG, "Updating task: " + task.getTitle() + " (ID: " + task.getTaskId() + ")");

        firebaseRepository.updateProjectTask(task, new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Task updated successfully in Firebase");

                // 🆕 Firebase 업데이트 성공 시 로컬 동기화는 자동으로 처리됨
                // CollaborationSyncService가 실시간으로 감지하여 로컬 DB에 반영
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("할 일 수정에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to update task", e);
            }
        });
    }

    public void deleteTask(String taskId) {
        Log.d(TAG, "Deleting task with ID: " + taskId);

        firebaseRepository.deleteProjectTask(taskId, new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                successMessage.setValue("할 일이 삭제되었습니다.");
                Log.d(TAG, "Task deleted successfully from Firebase");

                // 🆕 Firebase에서 삭제되면 CollaborationSyncService가 자동으로
                // 로컬 DB에서도 해당 할 일을 제거합니다.
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("할 일 삭제에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to delete task", e);
            }
        });
    }

    public void toggleTaskCompletion(ProjectTask task) {
        // 현재 상태를 로그로 확인
        Log.d(TAG, "Toggling task completion. Current state: " + task.isCompleted() + " for task: " + task.getTitle());

        task.setCompleted(!task.isCompleted());

        // 변경된 상태 로그
        Log.d(TAG, "New state after toggle: " + task.isCompleted());

        updateTask(task);
    }

    // 🆕 로컬 DB 관련 메서드들

    /**
     * 현재 프로젝트의 로컬 동기화된 할 일들 조회
     */
    public LiveData<List<com.am.mytodolistapp.data.TodoDao.TodoWithCategoryInfo>> getLocalSyncedTodos() {
        if (currentProjectId != null) {
            return todoRepository.getTodosByProject(currentProjectId);
        }
        return null;
    }

    /**
     * 동기화 상태 확인
     */
    public boolean isSyncActive() {
        return todoRepository.isCollaborationSyncActive();
    }

    /**
     * 수동 동기화 실행
     */
    public void performManualSync() {
        Log.d(TAG, "Performing manual sync for project: " + currentProjectId);
        todoRepository.performManualSync();
    }

    /**
     * 협업 할 일 개수 조회
     */
    public void getCollaborationTodoCount(OnCountListener listener) {
        todoRepository.getCollaborationTodoCount(listener::onCount);
    }

    /**
     * 현재 프로젝트 정보와 동기화 상태 로그 출력
     */
    public void logProjectInfo() {
        Log.d(TAG, "=== Project Info ===");
        Log.d(TAG, "Current project ID: " + currentProjectId);
        Log.d(TAG, "Sync active: " + todoRepository.isCollaborationSyncActive());
        Log.d(TAG, "Syncing projects: " + todoRepository.getSyncingProjectCount());

        Project project = currentProject.getValue();
        if (project != null) {
            Log.d(TAG, "Project name: " + project.getProjectName());
            Log.d(TAG, "Project members: " + (project.getMemberIds() != null ? project.getMemberIds().size() : 0));
        }
        Log.d(TAG, "===================");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared");

        // 🆕 ViewModel이 클리어될 때 필요한 정리 작업
        // TodoRepository의 리스너는 MainActivity에서 관리하므로 여기서는 특별한 정리 작업 없음
    }

    // 🆕 콜백 인터페이스
    public interface OnCountListener {
        void onCount(int count);
    }
}