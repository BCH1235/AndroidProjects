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

//ProjectTaskListFragment의 UI 상태와 비즈니스 로직을 관리하는 ViewModel

/* FirebaseRepository를 통해 특정 프로젝트의 할 일 데이터를 가져오고,
 할 일 추가, 수정, 삭제, 완료 상태 변경 등의 작업을 처리
 프로젝트 멤버 정보를 로드하는 기능도 제공 */
public class ProjectTaskListViewModel extends AndroidViewModel {
    private static final String TAG = "ProjectTaskListVM";

    private FirebaseRepository firebaseRepository;
    private TodoRepository todoRepository; // 로컬 DB 동기화를 위한 리포지토리
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

    //현재 작업할 프로젝트의 ID를 설정하고, 해당 프로젝트의 할 일 목록 로드를 시작
    public void setProjectId(String projectId) {
        this.currentProjectId = projectId;
        loadProjectTasks();

        Log.d(TAG, "Project ID set: " + projectId);
    }

    //FirebaseRepository를 통해 현재 프로젝트의 할 일 목록을 실시간으로 가져온다
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

        // 프로젝트 상세 정보 가져오기
        firebaseRepository.getProjectDetails(currentProjectId, new FirebaseRepository.OnCompleteListener<Project>() {
            @Override
            public void onSuccess(Project project) {
                if (project != null && project.getMemberIds() != null && !project.getMemberIds().isEmpty()) {
                    Log.d(TAG, "Project loaded, member count: " + project.getMemberIds().size());

                    // 프로젝트 정보 저장
                    currentProject.setValue(project);

                    // 멤버 ID들로 사용자 정보 가져오기
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

    //새로운 할 일을 프로젝트에 추가
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
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("할 일 수정에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to update task", e);
            }
        });
    } // 기존 할 일 정보를 업데이트

    public void deleteTask(String taskId) {
        Log.d(TAG, "Deleting task with ID: " + taskId);

        firebaseRepository.deleteProjectTask(taskId, new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                successMessage.setValue("할 일이 삭제되었습니다.");
                Log.d(TAG, "Task deleted successfully from Firebase");
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("할 일 삭제에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to delete task", e);
            }
        });
    } // 할 일을 삭제

    public void toggleTaskCompletion(ProjectTask task) {
        // 현재 상태를 로그로 확인
        Log.d(TAG, "Toggling task completion. Current state: " + task.isCompleted() + " for task: " + task.getTitle());

        task.setCompleted(!task.isCompleted());

        // 변경된 상태 로그
        Log.d(TAG, "New state after toggle: " + task.isCompleted());

        updateTask(task);
    } // 할 일의 완료 상태를 토글


    public LiveData<List<com.am.mytodolistapp.data.TodoDao.TodoWithCategoryInfo>> getLocalSyncedTodos() {
        if (currentProjectId != null) {
            return todoRepository.getTodosByProject(currentProjectId);
        }
        return null;
    }

    public boolean isSyncActive() {
        return todoRepository.isCollaborationSyncActive();
    }

    public void performManualSync() {
        Log.d(TAG, "Performing manual sync for project: " + currentProjectId);
        todoRepository.performManualSync();
    }

    public void getCollaborationTodoCount(OnCountListener listener) {
        todoRepository.getCollaborationTodoCount(listener::onCount);
    } // 협업 할 일 개수 조회

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
    } //현재 프로젝트 정보와 동기화 상태 로그 출력

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared");

    }

    public interface OnCountListener {
        void onCount(int count);
    }
}