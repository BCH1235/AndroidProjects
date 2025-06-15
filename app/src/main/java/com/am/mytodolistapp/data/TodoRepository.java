package com.am.mytodolistapp.data;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.sync.CollaborationSyncService;

import java.util.List;


public class TodoRepository {

    private TodoDao mTodoDao;
    private LiveData<List<TodoItem>> mAllTodos;
    private CollaborationSyncService collaborationSyncService;
    private Application application;

    public TodoRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        mTodoDao = db.todoDao();
        mAllTodos = mTodoDao.getAllTodos();

        collaborationSyncService = CollaborationSyncService.getInstance(application);
    }

    // ========== 기존 메서드들 ==========
    public LiveData<List<TodoItem>> getAllTodos() {
        return mAllTodos;
    }

    public void insert(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.insert(todoItem);
        });
    }

    public void update(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.update(todoItem);

            // 🆕 협업 할 일인 경우 Firebase에도 동기화
            if (todoItem.isFromCollaboration()) {
                collaborationSyncService.syncCompletionToFirebase(todoItem);
            }
        });
    }

    public void delete(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.delete(todoItem);

            if (todoItem.isFromCollaboration() && todoItem.getFirebaseTaskId() != null) {

            }
        });
    }

    public void deleteAllTodos() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.deleteAllTodos();
        });
    }

    public TodoItem getTodoByIdSync(int id) {
        return mTodoDao.getTodoByIdSync(id);
    }

    // ========== 협업 관련 메서드들 ==========

    public void startCollaborationSync() {
        collaborationSyncService.startSyncForAllProjects();
    }

    public void stopCollaborationSync() {
        collaborationSyncService.stopAllSync();
    }

    public void performManualSync() {
        collaborationSyncService.performManualSync();
    }

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
        if (!todoItem.isFromCollaboration()) {
            // 일반 할 일인 경우 기존 로직 사용
            AppDatabase.databaseWriteExecutor.execute(() -> {
                TodoItem itemToUpdate = mTodoDao.getTodoByIdSync(todoItem.getId());
                if (itemToUpdate != null) {
                    itemToUpdate.setCompleted(!itemToUpdate.isCompleted());
                    mTodoDao.update(itemToUpdate);
                }
            });
            return;
        }

        // 협업 할 일인 경우 Firebase와 동기화
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TodoItem itemToUpdate = mTodoDao.getTodoByIdSync(todoItem.getId());
            if (itemToUpdate != null) {
                itemToUpdate.setCompleted(!itemToUpdate.isCompleted());
                mTodoDao.update(itemToUpdate);

                // Firebase에도 동기화
                collaborationSyncService.syncCompletionToFirebase(itemToUpdate);
            }
        });
    }

    public void getCollaborationTodoCount(CountCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = mTodoDao.countCollaborationTodos();
            // UI 스레드에서 콜백 실행
            application.getMainExecutor().execute(() -> callback.onCount(count));
        });
    }

    public interface CountCallback {
        void onCount(int count);
    }
}