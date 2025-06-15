package com.am.mytodolistapp.data.sync;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.TodoDao;
import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.am.mytodolistapp.data.firebase.Project;
import com.am.mytodolistapp.data.firebase.ProjectTask;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase ProjectTask와 Room TodoItem 간의 동기화를 담당하는 서비스 클래스
 */
public class CollaborationSyncService {
    private static final String TAG = "CollaborationSync";

    private static CollaborationSyncService instance;
    private FirebaseRepository firebaseRepository;
    private TodoDao todoDao;
    private Application application;

    // 프로젝트별 LiveData 관찰자들을 저장
    private Map<String, Observer<List<ProjectTask>>> projectObservers = new HashMap<>();
    private Map<String, LiveData<List<ProjectTask>>> projectTasksLiveData = new HashMap<>();
    private Observer<List<Project>> userProjectsObserver;
    private LiveData<List<Project>> userProjectsLiveData;

    public static synchronized CollaborationSyncService getInstance(Application application) {
        if (instance == null) {
            instance = new CollaborationSyncService(application);
        }
        return instance;
    }

    private CollaborationSyncService(Application application) {
        this.application = application;
        this.firebaseRepository = FirebaseRepository.getInstance();
        this.todoDao = AppDatabase.getDatabase(application).todoDao();
    }

    /**
     * 사용자의 모든 프로젝트에 대해 동기화 시작
     */
    public void startSyncForAllProjects() {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot start sync");
            return;
        }

        Log.d(TAG, "Starting sync for all projects for user: " + currentUser.getUid());

        // 기존 관찰자 정리
        stopAllSync();

        // 사용자의 프로젝트 목록을 가져와서 각 프로젝트 동기화 시작
        userProjectsLiveData = firebaseRepository.getUserProjects(currentUser.getUid());
        userProjectsObserver = new Observer<List<Project>>() {
            @Override
            public void onChanged(List<Project> projects) {
                if (projects != null) {
                    Log.d(TAG, "User projects changed, updating sync for " + projects.size() + " projects");

                    // 기존 프로젝트 동기화 중지
                    stopAllProjectSync();

                    // 새 프로젝트들에 대해 동기화 시작
                    for (Project project : projects) {
                        startSyncForProject(project.getProjectId(), project.getProjectName());
                    }
                } else {
                    Log.d(TAG, "No projects found for user");
                    stopAllProjectSync();
                }
            }
        };

        userProjectsLiveData.observeForever(userProjectsObserver);
    }

    /**
     * 특정 프로젝트에 대한 동기화 시작
     */
    public void startSyncForProject(String projectId, String projectName) {
        if (projectId == null) {
            Log.w(TAG, "Cannot start sync for null projectId");
            return;
        }

        Log.d(TAG, "Starting sync for project: " + projectId + " (" + projectName + ")");

        // 이미 동기화 중인 프로젝트라면 기존 관찰자 제거
        stopSyncForProject(projectId);

        // 프로젝트의 할 일 목록 실시간 관찰 시작
        LiveData<List<ProjectTask>> projectTasks = firebaseRepository.getProjectTasks(projectId);
        Observer<List<ProjectTask>> observer = new Observer<List<ProjectTask>>() {
            @Override
            public void onChanged(List<ProjectTask> tasks) {
                if (tasks != null) {
                    Log.d(TAG, "Received " + tasks.size() + " tasks for project " + projectId);
                    syncProjectTasksToRoom(tasks, projectName, projectId);
                } else {
                    Log.d(TAG, "No tasks found for project " + projectId);
                    // 프로젝트에 할 일이 없으면 기존 할 일들을 삭제할지 결정
                    // 여기서는 Firebase에서 삭제된 할 일만 로컬에서도 삭제하도록 함
                }
            }
        };

        projectTasks.observeForever(observer);
        projectObservers.put(projectId, observer);
        projectTasksLiveData.put(projectId, projectTasks);
    }

    /**
     * 특정 프로젝트의 동기화 중지
     */
    public void stopSyncForProject(String projectId) {
        Observer<List<ProjectTask>> observer = projectObservers.get(projectId);
        LiveData<List<ProjectTask>> liveData = projectTasksLiveData.get(projectId);

        if (observer != null && liveData != null) {
            liveData.removeObserver(observer);
            projectObservers.remove(projectId);
            projectTasksLiveData.remove(projectId);
            Log.d(TAG, "Stopped sync for project: " + projectId);
        }
    }

    /**
     * 모든 프로젝트의 동기화 중지
     */
    private void stopAllProjectSync() {
        for (String projectId : projectObservers.keySet()) {
            stopSyncForProject(projectId);
        }
        Log.d(TAG, "Stopped all project synchronization");
    }

    /**
     * 모든 동기화 중지 (프로젝트 목록 관찰 포함)
     */
    public void stopAllSync() {
        // 프로젝트별 동기화 중지
        stopAllProjectSync();

        // 사용자 프로젝트 목록 관찰 중지
        if (userProjectsObserver != null && userProjectsLiveData != null) {
            userProjectsLiveData.removeObserver(userProjectsObserver);
            userProjectsObserver = null;
            userProjectsLiveData = null;
        }

        Log.d(TAG, "Stopped all synchronization including user projects observer");
    }

    /**
     * Firebase ProjectTask 목록을 Room DB에 동기화
     */
    private void syncProjectTasksToRoom(List<ProjectTask> projectTasks, String projectName, String projectId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 1. 현재 프로젝트의 기존 로컬 할 일들 가져오기
                List<TodoItem> existingLocalTasks = todoDao.getTodosByProjectIdSync(projectId);
                Map<String, TodoItem> existingTasksMap = new HashMap<>();
                for (TodoItem item : existingLocalTasks) {
                    if (item.getFirebaseTaskId() != null) {
                        existingTasksMap.put(item.getFirebaseTaskId(), item);
                    }
                }

                // 2. Firebase의 할 일들을 하나씩 처리
                for (ProjectTask projectTask : projectTasks) {
                    syncSingleProjectTask(projectTask, projectName);
                    // 처리된 할 일은 맵에서 제거
                    existingTasksMap.remove(projectTask.getTaskId());
                }

                // 3. 맵에 남은 할 일들은 Firebase에서 삭제된 것이므로 로컬에서도 삭제
                for (TodoItem deletedTask : existingTasksMap.values()) {
                    Log.d(TAG, "Deleting locally removed task: " + deletedTask.getTitle());
                    todoDao.delete(deletedTask);
                }

                Log.d(TAG, "Successfully synced " + projectTasks.size() +
                        " tasks for project: " + projectName);
            } catch (Exception e) {
                Log.e(TAG, "Error syncing project tasks to Room", e);
            }
        });
    }

    /**
     * 단일 ProjectTask를 Room DB에 동기화
     */
    private void syncSingleProjectTask(ProjectTask projectTask, String projectName) {
        if (projectTask == null || projectTask.getTaskId() == null) {
            Log.w(TAG, "Invalid project task, skipping sync");
            return;
        }

        try {
            // 기존 TodoItem 확인
            TodoItem existingTodoItem = todoDao.getTodoByFirebaseTaskId(projectTask.getTaskId());

            if (existingTodoItem != null) {
                // 기존 아이템 업데이트
                if (!DataSyncUtil.isDataSynced(existingTodoItem, projectTask)) {
                    DataSyncUtil.updateTodoItemFromProjectTask(existingTodoItem, projectTask, projectName);
                    todoDao.update(existingTodoItem);
                    Log.d(TAG, "Updated existing todo: " + projectTask.getTitle());
                }
            } else {
                // 새 아이템 생성
                TodoItem newTodoItem = DataSyncUtil.convertProjectTaskToTodoItem(projectTask, projectName);
                if (newTodoItem != null) {
                    todoDao.insert(newTodoItem);
                    Log.d(TAG, "Inserted new todo: " + projectTask.getTitle());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing single project task: " + projectTask.getTitle(), e);
        }
    }

    /**
     * 협업 할 일의 완료 상태를 Firebase에 동기화
     */
    public void syncCompletionToFirebase(TodoItem todoItem) {
        if (!todoItem.isFromCollaboration() || todoItem.getFirebaseTaskId() == null) {
            return;
        }

        Log.d(TAG, "Syncing completion status to Firebase: " + todoItem.getTitle() + " -> " + todoItem.isCompleted());

        ProjectTask projectTask = DataSyncUtil.convertTodoItemToProjectTask(todoItem);
        if (projectTask != null) {
            firebaseRepository.updateProjectTask(projectTask, new FirebaseRepository.OnCompleteListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Successfully synced completion status to Firebase: " + todoItem.getTitle());
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to sync completion status to Firebase: " + todoItem.getTitle(), e);
                }
            });
        }
    }

    /**
     * 협업 할 일의 전체 데이터를 Firebase에 동기화
     */
    public void syncTodoItemToFirebase(TodoItem todoItem) {
        if (!todoItem.isFromCollaboration() || todoItem.getFirebaseTaskId() == null) {
            return;
        }

        Log.d(TAG, "Syncing todo item to Firebase: " + todoItem.getTitle());

        ProjectTask projectTask = DataSyncUtil.convertTodoItemToProjectTask(todoItem);
        if (projectTask != null) {
            firebaseRepository.updateProjectTask(projectTask, new FirebaseRepository.OnCompleteListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Successfully synced todo item to Firebase: " + todoItem.getTitle());
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to sync todo item to Firebase: " + todoItem.getTitle(), e);
                }
            });
        }
    }

    /**
     * 삭제된 ProjectTask에 대응하여 Room DB에서도 삭제
     */
    public void handleProjectTaskDeletion(String firebaseTaskId) {
        if (firebaseTaskId == null) {
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                todoDao.deleteByFirebaseTaskId(firebaseTaskId);
                Log.d(TAG, "Deleted todo item for Firebase task: " + firebaseTaskId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting todo item for Firebase task", e);
            }
        });
    }

    /**
     * 프로젝트가 삭제될 때 관련된 모든 TodoItem 삭제
     */
    public void handleProjectDeletion(String projectId) {
        if (projectId == null) {
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                todoDao.deleteAllTodosByProjectId(projectId);
                Log.d(TAG, "Deleted all todo items for project: " + projectId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting todo items for project", e);
            }
        });

        // 해당 프로젝트의 동기화도 중지
        stopSyncForProject(projectId);
    }

    /**
     * 수동 동기화 (앱 시작 시 또는 사용자 요청 시)
     */
    public void performManualSync() {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot perform manual sync");
            return;
        }

        Log.d(TAG, "Performing manual sync...");

        // 기존 동기화 중지 후 재시작
        stopAllSync();
        startSyncForAllProjects();
    }

    /**
     * 현재 동기화 상태 확인
     */
    public boolean isSyncActive() {
        return userProjectsObserver != null && !projectObservers.isEmpty();
    }

    /**
     * 동기화 중인 프로젝트 수 반환
     */
    public int getSyncingProjectCount() {
        return projectObservers.size();
    }
}