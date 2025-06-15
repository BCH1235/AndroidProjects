package com.am.mytodolistapp.data;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.sync.CollaborationSyncService;

import java.util.List;

/**
 * 앱의 할 일 데이터 관리 총괄 (로컬 + 협업 통합)
 */
public class TodoRepository {
    private static final String TAG = "TodoRepository";

    private TodoDao mTodoDao;
    private LiveData<List<TodoItem>> mAllTodos;
    private CollaborationSyncService collaborationSyncService;
    private Application application;

    public TodoRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        mTodoDao = db.todoDao();
        mAllTodos = mTodoDao.getAllTodos();

        // 협업 동기화 서비스 초기화
        collaborationSyncService = CollaborationSyncService.getInstance(application);

        Log.d(TAG, "TodoRepository initialized");
    }

    // ========== 기존 메서드들 (수정) ==========

    public LiveData<List<TodoItem>> getAllTodos() {
        return mAllTodos;
    }

    public void insert(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long insertedId = mTodoDao.insertAndGetId(todoItem);
            Log.d(TAG, "Inserted new todo with ID: " + insertedId + ", title: " + todoItem.getTitle());
        });
    }

    public void update(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.update(todoItem);
            Log.d(TAG, "Updated todo: " + todoItem.getTitle());

            // 협업 할 일인 경우 Firebase에도 동기화
            if (todoItem.isFromCollaboration()) {
                Log.d(TAG, "Syncing collaboration todo update to Firebase: " + todoItem.getTitle());
                collaborationSyncService.syncTodoItemToFirebase(todoItem);
            }
        });
    }

    public void delete(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.delete(todoItem);
            Log.d(TAG, "Deleted todo: " + todoItem.getTitle());

            // 협업 할 일 삭제 시 주의사항:
            // 여기서는 로컬에서만 삭제하고 Firebase는 그대로 둡니다.
            // Firebase에서 삭제하면 다른 팀원들에게도 영향을 주기 때문입니다.
            // 필요 시 별도의 "협업 할 일 숨기기" 기능을 구현할 수 있습니다.
            if (todoItem.isFromCollaboration()) {
                Log.d(TAG, "Deleted collaboration todo locally (not syncing deletion to Firebase): " + todoItem.getTitle());
            }
        });
    }

    public void deleteAllTodos() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.deleteAllTodos();
            Log.d(TAG, "Deleted all todos");
        });
    }

    public TodoItem getTodoByIdSync(int id) {
        return mTodoDao.getTodoByIdSync(id);
    }

    // ========== 협업 관련 메서드들 (새로 추가) ==========

    /**
     * 협업 동기화 시작 (앱 시작 시 호출)
     */
    public void startCollaborationSync() {
        Log.d(TAG, "Starting collaboration sync...");
        try {
            collaborationSyncService.startSyncForAllProjects();
            Log.d(TAG, "Collaboration sync started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting collaboration sync", e);
        }
    }

    /**
     * 협업 동기화 중지 (앱 종료 시 호출)
     */
    public void stopCollaborationSync() {
        Log.d(TAG, "Stopping collaboration sync...");
        try {
            collaborationSyncService.stopAllSync();
            Log.d(TAG, "Collaboration sync stopped successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping collaboration sync", e);
        }
    }

    /**
     * 수동 동기화 수행
     */
    public void performManualSync() {
        Log.d(TAG, "Performing manual sync...");
        try {
            collaborationSyncService.performManualSync();
            Log.d(TAG, "Manual sync completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error performing manual sync", e);
        }
    }

    /**
     * 협업 할 일들만 조회
     */
    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getCollaborationTodos() {
        return mTodoDao.getCollaborationTodosWithCategory();
    }

    /**
     * 로컬 할 일들만 조회 (협업 제외)
     */
    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getLocalTodos() {
        return mTodoDao.getLocalTodosWithCategory();
    }

    /**
     * 특정 프로젝트의 할 일들 조회
     */
    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getTodosByProject(String projectId) {
        return mTodoDao.getTodosByProjectWithCategory(projectId);
    }

    /**
     * 협업 할 일 완료 상태 토글 (Firebase와 동기화)
     */
    public void toggleCollaborationTodoCompletion(TodoItem todoItem) {
        Log.d(TAG, "Toggling collaboration todo completion: " + todoItem.getTitle() + " -> " + !todoItem.isCompleted());

        if (!todoItem.isFromCollaboration()) {
            // 일반 할 일인 경우 기존 로직 사용
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

        // 협업 할 일인 경우 Firebase와 동기화
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TodoItem itemToUpdate = mTodoDao.getTodoByIdSync(todoItem.getId());
            if (itemToUpdate != null) {
                boolean newCompletionStatus = !itemToUpdate.isCompleted();
                itemToUpdate.setCompleted(newCompletionStatus);
                mTodoDao.update(itemToUpdate);

                Log.d(TAG, "Updated collaboration todo completion locally: " + itemToUpdate.getTitle() + " -> " + newCompletionStatus);

                // Firebase에도 동기화 (완료 상태만)
                collaborationSyncService.syncCompletionToFirebase(itemToUpdate);
            }
        });
    }

    /**
     * 협업 할 일 개수 조회
     */
    public void getCollaborationTodoCount(CountCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = mTodoDao.countCollaborationTodos();
            Log.d(TAG, "Collaboration todo count: " + count);
            // UI 스레드에서 콜백 실행
            if (application != null) {
                application.getMainExecutor().execute(() -> callback.onCount(count));
            }
        });
    }

    /**
     * 모든 협업 할 일 삭제 (로그아웃 시 사용)
     */
    public void deleteAllCollaborationTodos() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int deletedCount = mTodoDao.countCollaborationTodos();
            mTodoDao.deleteAllCollaborationTodos();
            Log.d(TAG, "Deleted all collaboration todos: " + deletedCount + " items");
        });
    }

    /**
     * 특정 사용자가 생성한 협업 할 일들 조회
     */
    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getCollaborationTodosByCreator(String userId) {
        return mTodoDao.getCollaborationTodosByCreator(userId);
    }

    /**
     * 특정 사용자에게 할당된 협업 할 일들 조회
     */
    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getCollaborationTodosByAssignee(String userId) {
        return mTodoDao.getCollaborationTodosByAssignee(userId);
    }

    /**
     * 우선순위별 협업 할 일들 조회
     */
    public LiveData<List<TodoDao.TodoWithCategoryInfo>> getCollaborationTodosByPriority(String priority) {
        return mTodoDao.getCollaborationTodosByPriority(priority);
    }

    /**
     * 프로젝트별 완료율 조회
     */
    public void getProjectCompletionRates(ProjectCompletionCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<TodoDao.ProjectCompletionRate> rates = mTodoDao.getProjectCompletionRates();
            Log.d(TAG, "Retrieved completion rates for " + rates.size() + " projects");
            if (application != null) {
                application.getMainExecutor().execute(() -> callback.onRates(rates));
            }
        });
    }

    /**
     * 동기화 상태 확인
     */
    public boolean isCollaborationSyncActive() {
        return collaborationSyncService.isSyncActive();
    }

    /**
     * 동기화 중인 프로젝트 수 반환
     */
    public int getSyncingProjectCount() {
        return collaborationSyncService.getSyncingProjectCount();
    }

    /**
     * 협업 관련 정보 로그 출력 (디버깅용)
     */
    public void logCollaborationInfo() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int totalTodos = mTodoDao.getAllCollaborationTodosSync().size();
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

    // ========== 콜백 인터페이스들 ==========

    /**
     * 개수 조회 콜백 인터페이스
     */
    public interface CountCallback {
        void onCount(int count);
    }

    /**
     * 프로젝트 완료율 콜백 인터페이스
     */
    public interface ProjectCompletionCallback {
        void onRates(List<TodoDao.ProjectCompletionRate> rates);
    }
}