package com.am.mytodolistapp.ui.collaboration;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.am.mytodolistapp.data.firebase.ProjectTask;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ProjectTaskListViewModel extends AndroidViewModel {
    private static final String TAG = "ProjectTaskListVM";

    private FirebaseRepository firebaseRepository;
    private String currentProjectId;
    private LiveData<List<ProjectTask>> projectTasks;
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> successMessage = new MutableLiveData<>();

    public ProjectTaskListViewModel(@NonNull Application application) {
        super(application);
        firebaseRepository = FirebaseRepository.getInstance();
    }

    public void setProjectId(String projectId) {
        this.currentProjectId = projectId;
        loadProjectTasks();
    }

    private void loadProjectTasks() {
        if (currentProjectId != null) {
            projectTasks = firebaseRepository.getProjectTasks(currentProjectId);
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

    // 메시지를 리셋하는 메서드들
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    public void clearSuccessMessage() {
        successMessage.setValue(null);
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
        firebaseRepository.updateProjectTask(task, new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {

                Log.d(TAG, "Task updated successfully");
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("할 일 수정에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to update task", e);
            }
        });
    }

    public void deleteTask(String taskId) {
        firebaseRepository.deleteProjectTask(taskId, new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                successMessage.setValue("할 일이 삭제되었습니다.");
                Log.d(TAG, "Task deleted successfully");
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

    @Override
    protected void onCleared() {
        super.onCleared();
        firebaseRepository.removeAllListeners();
    }
}