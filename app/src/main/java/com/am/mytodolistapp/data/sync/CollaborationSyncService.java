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

public class CollaborationSyncService {
    private static final String TAG = "CollaborationSync";

    private static CollaborationSyncService instance;
    private FirebaseRepository firebaseRepository;
    private TodoDao todoDao;
    private Application application;

    // 프로젝트별 LiveData 관찰자들을 저장
    private Map<String, Observer<List<ProjectTask>>> projectObservers = new HashMap<>();
    private Map<String, LiveData<List<ProjectTask>>> projectTasksLiveData = new HashMap<>();

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

    public void startSyncForAllProjects() {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot start sync");
            return;
        }

        Log.d(TAG, "Starting sync for all projects for user: " + currentUser.getUid());

        // 사용자의 프로젝트 목록을 가져와서 각 프로젝트 동기화 시작
        LiveData<List<Project>> userProjects = firebaseRepository.getUserProjects(currentUser.getUid());
        userProjects.observeForever(new Observer<List<Project>>() {
            @Override
            public void onChanged(List<Project> projects) {
                if (projects != null) {
                    for (Project project : projects) {
                        startSyncForProject(project.getProjectId(), project.getProjectName());
                    }
                }
            }
        });
    }

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
                    syncProjectTasksToRoom(tasks, projectName);
                }
            }
        };

        projectTasks.observeForever(observer);
        projectObservers.put(projectId, observer);
        projectTasksLiveData.put(projectId, projectTasks);
    }

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

    public void stopAllSync() {
        for (String projectId : projectObservers.keySet()) {
            stopSyncForProject(projectId);
        }
        Log.d(TAG, "Stopped all project synchronization");
    }

    private void syncProjectTasksToRoom(List<ProjectTask> projectTasks, String projectName) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                for (ProjectTask projectTask : projectTasks) {
                    syncSingleProjectTask(projectTask, projectName);
                }
                Log.d(TAG, "Successfully synced " + projectTasks.size() +
                        " tasks for project: " + projectName);
            } catch (Exception e) {
                Log.e(TAG, "Error syncing project tasks to Room", e);
            }
        });
    }

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

    public void syncCompletionToFirebase(TodoItem todoItem) {
        if (!todoItem.isFromCollaboration() || todoItem.getFirebaseTaskId() == null) {
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Firebase에서 해당 할 일을 찾아서 완료 상태 업데이트
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
            } catch (Exception e) {
                Log.e(TAG, "Error syncing completion to Firebase", e);
            }
        });
    }

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
}