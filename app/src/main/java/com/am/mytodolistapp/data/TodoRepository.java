package com.am.mytodolistapp.data;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.sync.CollaborationSyncService;

import java.util.List;

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

        collaborationSyncService = CollaborationSyncService.getInstance(application);
        Log.d(TAG, "TodoRepository initialized");
    }

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

    public void startCollaborationSync() {
        Log.d(TAG, "Starting collaboration sync...");
        try {
            collaborationSyncService.startSyncForAllProjects();
            Log.d(TAG, "Collaboration sync started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting collaboration sync", e);
        }
    }

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
        Log.d(TAG, "Toggling collaboration todo completion: " + todoItem.getTitle() + " -> " + !todoItem.isCompleted());

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
    }

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