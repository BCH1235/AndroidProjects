package com.am.mytodolistapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.LocationDao;
import com.am.mytodolistapp.data.LocationItem;
import com.am.mytodolistapp.data.TodoDao;
import com.am.mytodolistapp.data.TodoItem;

import java.util.List;

public class LocationBasedTaskViewModel extends AndroidViewModel {

    private LocationDao locationDao;
    private TodoDao todoDao;
    private LiveData<List<LocationItem>> allLocations;

    public LocationBasedTaskViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        locationDao = db.locationDao();
        todoDao = db.todoDao();
        allLocations = locationDao.getAllLocations();
    }

    // 위치 관련 메서드들
    public LiveData<List<LocationItem>> getAllLocations() {
        return allLocations;
    }

    public void insertLocation(LocationItem location) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            locationDao.insert(location);
        });
    }

    public void updateLocation(LocationItem location) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            locationDao.update(location);
        });
    }

    public void deleteLocation(LocationItem location) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            locationDao.delete(location);
        });
    }

    // 특정 위치의 할 일들 가져오기
    public LiveData<List<TodoItem>> getTodosByLocationId(int locationId) {
        return todoDao.getTodosByLocationId(locationId);
    }

    // 할 일 관련 메서드들
    public void insertTodo(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.insert(todoItem);
        });
    }

    public void updateTodo(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.update(todoItem);
        });
    }

    public void deleteTodo(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.delete(todoItem);
        });
    }
}