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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* 1.Firebase(원격)와 Room(로컬) 데이터베이스 간의 데이터 동기화를 총괄하는 핵심 클래스
   2.실시간으로 Firebase의 데이터 변경을 감지하여 로컬 데이터베이스에 반영하는 역할을 한다. */

/* TodoRepository: 서비스의 시작/중지/수동 동기화를 제어
   FirebaseRepository: Firebase의 실시간 데이터를 가져오기 위해 사용
   AppDatabase (TodoDao): 로컬 데이터베이스에 변경 사항을 반영하기 위해 사용
   DataSyncUtil: Firebase 객체와 Room 객체 간의 데이터 변환을 돕는다. */

public class CollaborationSyncService {
    private static final String TAG = "CollaborationSync";

    private static CollaborationSyncService instance;
    private FirebaseRepository firebaseRepository;
    private TodoDao todoDao;
    private Application application;

    // 프로젝트별 LiveData 옵저버들을 저장
    private Map<String, Observer<List<ProjectTask>>> projectObservers = new HashMap<>();
    private Map<String, LiveData<List<ProjectTask>>> projectTasksLiveData = new HashMap<>();
    private Observer<List<Project>> userProjectsObserver; // 사용자 프로젝트 목록 자체를 감시하는 옵저버
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

    // 사용자가 참여한 모든 프로젝트에 대한 동기화를 시작하는 메인 메소드
    // 로그인 성공 시 `TodoRepository`를 통해 호출된다
    // 현재 로그인된 사용자를 확인하고 실행 중이던 모든 동기화 리스너를 `stopAllSync()`로 정리한다.
    // `firebaseRepository.getUserProjects()`를 통해 사용자의 프로젝트 목록을 실시간으로 관찰하고 프로젝트 목록이 변경될 때마다, 모든 프로젝트의 할 일 동기화한다.
    public void startSyncForAllProjects() {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot start sync");
            return;
        }

        Log.d(TAG, "Starting sync for all projects for user: " + currentUser.getUid());


        stopAllSync(); // 시작하기 전에  이전 리스너들을 깨끗이 정리

        // 사용자의 프로젝트 목록을 가져와서 각 프로젝트 동기화 시작
        userProjectsLiveData = firebaseRepository.getUserProjects(currentUser.getUid());
        userProjectsObserver = new Observer<List<Project>>() {
            @Override
            public void onChanged(List<Project> projects) {
                if (projects != null) {
                    Log.d(TAG, "User projects changed, updating sync for " + projects.size() + " projects");


                    stopAllProjectSync(); // 기존 할 일 리스너들만 정리

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

        userProjectsLiveData.observeForever(userProjectsObserver); // observeForever: Activity/Fragment 생명주기와 무관하게 앱이 실행되는 동안 계속 관찰한다.
    }


    //특정 프로젝트에 대한 동기화 시작
    public void startSyncForProject(String projectId, String projectName) {
        if (projectId == null) {
            Log.w(TAG, "Cannot start sync for null projectId");
            return;
        }

        Log.d(TAG, "Starting sync for project: " + projectId + " (" + projectName + ")");


        stopSyncForProject(projectId); // 해당 프로젝트에 대한 기존 리스너가 있다면 먼저 제거한다.

        // 프로젝트의 할 일 목록 실시간 관찰 시작
        LiveData<List<ProjectTask>> projectTasks = firebaseRepository.getProjectTasks(projectId);
        Observer<List<ProjectTask>> observer = new Observer<List<ProjectTask>>() {
            @Override
            public void onChanged(List<ProjectTask> tasks) {
                if (tasks != null) {
                    Log.d(TAG, "Received " + tasks.size() + " tasks for project " + projectId);
                    syncProjectTasksToRoom(tasks, projectName, projectId); // Firebase에서 데이터 변경이 감지되면, 로컬 DB와 동기화하는 메소드 호출
                } else {
                    Log.d(TAG, "No tasks found for project " + projectId);
                    // 프로젝트에 할 일이 없으면 기존 할 일들을 삭제할지 결정
                    // 여기서는 Firebase에서 삭제된 할 일만 로컬에서도 삭제하도록 함
                }
            }
        };

        projectTasks.observeForever(observer);
        // 관리 맵에 새로 생성된 리스너와 LiveData를 저장
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
    } // 특정 프로젝트의 동기화를 중지

    private void stopAllProjectSync() {
        for (String projectId : new ArrayList<>(projectObservers.keySet())) {
            stopSyncForProject(projectId);
        }
        Log.d(TAG, "Stopped all project synchronization");
    } // 모든 프로젝트의 할 일 동기화를 중지


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
    } // 앱의 모든 동기화 관련 리스너를 중지하고 정리





    // Firebase에서 받은 할 일 목록을 로컬 Room DB에 동기화하는 핵심 로직
    // 현재 로컬 DB에 저장된 해당 프로젝트의 할 일들을 가져와 맵에 저장하고 Firebase에서 받은 새 할 일 목록을 하나씩 순회
    //각 할 일에 대해 `syncSingleProjectTask`를 호출하여 로컬 DB에 추가하거나 업데이트
    //처리된 할 일은 1번에서 만든 맵에서 제거합니다.
    //순회가 끝난 후 맵에 여전히 남아있는 할 일은 Firebase에서는 삭제되었지만 로컬에는 남아있는 것이므로, 로컬 DB에서도 삭제한다.

    private void syncProjectTasksToRoom(List<ProjectTask> projectTasks, String projectName, String projectId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                //현재 프로젝트의 기존 로컬 할 일들 가져오기
                List<TodoItem> existingLocalTasks = todoDao.getTodosByProjectIdSync(projectId);
                Map<String, TodoItem> existingTasksMap = new HashMap<>();
                for (TodoItem item : existingLocalTasks) {
                    if (item.getFirebaseTaskId() != null) {
                        existingTasksMap.put(item.getFirebaseTaskId(), item);
                    }
                }

                //Firebase의 할 일들을 하나씩 처리
                for (ProjectTask projectTask : projectTasks) {
                    syncSingleProjectTask(projectTask, projectName);
                    // 처리된 할 일은 맵에서 제거
                    existingTasksMap.remove(projectTask.getTaskId());
                }

                //맵에 남은 할 일들은 Firebase에서 삭제된 것이므로 로컬에서도 삭제
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
    } // 로컬에서 변경된 협업 할 일의 완료 상태를 Firebase에 전송


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
    } // 협업 할 일의 전체 데이터를 Firebase에 동기화


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
        stopSyncForProject(projectId);
    } // Firebase에서 프로젝트가 삭제되었을 때, 관련된 로컬 할 일들을 모두 삭제


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
    } // 수동 동기화 (앱 시작할 때, 사용자 요청 시)

    public boolean isSyncActive() {
        return userProjectsObserver != null && !projectObservers.isEmpty();
    } // 현재 동기화 상태 확인

    public int getSyncingProjectCount() {
        return projectObservers.size();
    } // 동기화 중인 프로젝트 수 반환
}