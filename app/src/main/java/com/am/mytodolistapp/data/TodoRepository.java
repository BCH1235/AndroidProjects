package com.am.mytodolistapp.data;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.sync.CollaborationSyncService;
import com.am.mytodolistapp.ui.location.LocationBasedTaskViewModel;
import com.am.mytodolistapp.ui.task.TaskListViewModel;

import java.util.List;

// 앱의 데이터 소스를 관리하고 비즈니스 로직을 처리하는 Repository 클래스
// ViewModel과 데이터 소스(Room, Firebase 등) 사이의 중개자 역할

// ViewModel은 데이터가 로컬 DB에서 오는지, 원격 서버에서 오는지 알 필요 없이 Repository에만 데이터를 요청한다.
// 모든 데이터 관련 로직(CRUD, 동기화 제어 등)이 이 클래스에 모여있어 관리가 용이

// 모든 데이터베이스 작업은 `AppDatabase.databaseWriteExecutor`를 통해 백그라운드 스레드에서 실행

/* TaskListViewModel, LocationBasedTaskViewModel 등: 이 Repository를 통해 데이터를 요청하고 UI에 필요한 데이터를 얻는다.
   AppDatabase, TodoDao: 로컬 데이터베이스에 접근하기 위해 사용
   CollaborationSyncService: Firebase와의 데이터 동기화 로직을 실행하기 위해 사용 */

public class TodoRepository {
    private static final String TAG = "TodoRepository";

    private TodoDao mTodoDao; // 로컬 할 일 데이터에 접근하기 위한 DAO
    private LiveData<List<TodoItem>> mAllTodos; // 모든 할 일 목록을 관찰 가능한 LiveData
    private CollaborationSyncService collaborationSyncService; // 협업 데이터 동기화
    private Application application;

    public TodoRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        mTodoDao = db.todoDao();
        mAllTodos = mTodoDao.getAllTodos();  // 모든 할 일 목록 LiveData 초기화

        // 협업 동기화 서비스
        collaborationSyncService = CollaborationSyncService.getInstance(application);
        Log.d(TAG, "TodoRepository initialized");
    }

    public LiveData<List<TodoItem>> getAllTodos() {
        return mAllTodos;
    }  //모든 할 일 목록을 LiveData 형태로 반환하고 ViewModel은 이 LiveData를 관찰하여 UI를 업데이트한다.

    public void insert(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long insertedId = mTodoDao.insertAndGetId(todoItem);
            Log.d(TAG, "Inserted new todo with ID: " + insertedId + ", title: " + todoItem.getTitle());
        });
    } // 새로운 할 일을 로컬 데이터베이스에 삽입

    public void update(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.update(todoItem);
            Log.d(TAG, "Updated todo: " + todoItem.getTitle());
            if (todoItem.isFromCollaboration()) {
                Log.d(TAG, "Syncing collaboration todo update to Firebase: " + todoItem.getTitle());
                collaborationSyncService.syncTodoItemToFirebase(todoItem);
            }
        });
    } // 기존 할 일을 업데이트

    public void delete(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.delete(todoItem);
            Log.d(TAG, "Deleted todo: " + todoItem.getTitle());
            if (todoItem.isFromCollaboration()) {
                Log.d(TAG, "Deleted collaboration todo locally (not syncing deletion to Firebase): " + todoItem.getTitle());
            }
        });
    } // 할 일을 삭제

    public void deleteAllTodos() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.deleteAllTodos();
            Log.d(TAG, "Deleted all todos");
        });
    } // 모든 할 일을 삭제

    public TodoItem getTodoByIdSync(int id) {
        return mTodoDao.getTodoByIdSync(id);
    }

    public void startCollaborationSync() {
        Log.d(TAG, "Starting collaboration sync...");
        try {
            collaborationSyncService.startSyncForAllProjects();
            Log.d(TAG, "Collaboration sync started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting collaboration sync", e);
        }
    } // Firebase와의 협업 데이터 동기화

    public void stopCollaborationSync() {
        Log.d(TAG, "Stopping collaboration sync...");
        try {
            collaborationSyncService.stopAllSync();
            Log.d(TAG, "Collaboration sync stopped successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping collaboration sync", e);
        }
    }

    public void performManualSync() {
        Log.d(TAG, "Performing manual sync...");
        try {
            collaborationSyncService.performManualSync();
            Log.d(TAG, "Manual sync completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error performing manual sync", e);
        }
    } // 수동으로 데이터 동기화 요청

    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getCollaborationTodos() {
        return mTodoDao.getCollaborationTodosWithCategory();
    }

    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getLocalTodos() {
        return mTodoDao.getLocalTodosWithCategory();
    }

    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getTodosByProject(String projectId) {
        return mTodoDao.getTodosByProjectWithCategory(projectId);
    }

    public void toggleCollaborationTodoCompletion(TodoItem todoItem) {
        Log.d(TAG, "Toggling collaboration todo completion: " + todoItem.getTitle() + " -> " + !todoItem.isCompleted());

        // 개인 할 일인 경우 로컬 DB만 업데이트
        if (!todoItem.isFromCollaboration()) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                TodoItem itemToUpdate = mTodoDao.getTodoByIdSync(todoItem.getId());
                if (itemToUpdate != null) {
                    itemToUpdate.setCompleted(!itemToUpdate.isCompleted());
                    mTodoDao.update(itemToUpdate);
                    Log.d(TAG, "Updated local todo completion: " + itemToUpdate.getTitle());
                }
            });
            return;
        }
        // 협업 할 일인 경우 로컬 DB 업데이트 후 Firebase에 동기화
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TodoItem itemToUpdate = mTodoDao.getTodoByIdSync(todoItem.getId());
            if (itemToUpdate != null) {
                boolean newCompletionStatus = !itemToUpdate.isCompleted();
                itemToUpdate.setCompleted(newCompletionStatus);
                mTodoDao.update(itemToUpdate);
                Log.d(TAG, "Updated collaboration todo completion locally: " + itemToUpdate.getTitle() + " -> " + newCompletionStatus);
                collaborationSyncService.syncCompletionToFirebase(itemToUpdate);
            }
        });
    } // 할 일의 완료 상태를 토글

    public void getCollaborationTodoCount(CountCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = mTodoDao.countCollaborationTodos();
            Log.d(TAG, "Collaboration todo count: " + count);
            if (application != null) {
                application.getMainExecutor().execute(() -> callback.onCount(count));
            }
        });
    }

    public void deleteAllCollaborationTodos() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int deletedCount = mTodoDao.countCollaborationTodos();
            mTodoDao.deleteAllCollaborationTodos();
            Log.d(TAG, "Deleted all collaboration todos: " + deletedCount + " items");
        });
    }


    // --- 기타 조회 및 정보 확인 메소드들 ---
    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getCollaborationTodosByCreator(String userId) {
        return mTodoDao.getCollaborationTodosByCreator(userId);
    }

    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getCollaborationTodosByAssignee(String userId) {
        return mTodoDao.getCollaborationTodosByAssignee(userId);
    }

    public void getProjectCompletionRates(ProjectCompletionCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<TodoDao.ProjectCompletionRate> rates = mTodoDao.getProjectCompletionRates();
            Log.d(TAG, "Retrieved completion rates for " + rates.size() + " projects");
            if (application != null) {
                application.getMainExecutor().execute(() -> callback.onRates(rates));
            }
        });
    }

    public boolean isCollaborationSyncActive() {
        return collaborationSyncService.isSyncActive();
    }

    public int getSyncingProjectCount() {
        return collaborationSyncService.getSyncingProjectCount();
    }

    public void logCollaborationInfo() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int collaborationCount = mTodoDao.countCollaborationTodos();
            boolean syncActive = collaborationSyncService.isSyncActive();
            int syncingProjects = collaborationSyncService.getSyncingProjectCount();

            Log.d(TAG, "=== Collaboration Info ===");
            Log.d(TAG, "Total collaboration todos: " + collaborationCount);
            Log.d(TAG, "Sync active: " + syncActive);
            Log.d(TAG, "Syncing projects: " + syncingProjects);
            Log.d(TAG, "=========================");
        });
    }

    public interface CountCallback {
        void onCount(int count);
    }

    public interface ProjectCompletionCallback {
        void onRates(List<TodoDao.ProjectCompletionRate> rates);
    }
}