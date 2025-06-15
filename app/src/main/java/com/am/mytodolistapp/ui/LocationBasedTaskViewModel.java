package com.am.mytodolistapp.ui;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.LocationDao;
import com.am.mytodolistapp.data.LocationItem;
import com.am.mytodolistapp.data.TodoDao;
import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.service.LocationService;

import java.util.List;

public class LocationBasedTaskViewModel extends AndroidViewModel {
    private static final String TAG = "LocationTaskViewModel";

    private LocationDao locationDao;
    private TodoDao todoDao;
    private LiveData<List<LocationItem>> allLocations;
    private LocationService locationService;

    public LocationBasedTaskViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        locationDao = db.locationDao();
        todoDao = db.todoDao();
        allLocations = locationDao.getAllLocations();
        locationService = new LocationService(application);

        // 앱 시작 시 기존 위치 기반 할 일들에 대해 Geofence 등록
        initializeGeofences();
    }

    // 앱 시작 시 기존 위치 기반 할 일들에 대해 Geofence 등록
    private void initializeGeofences() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<TodoItem> activeLocationTodos = todoDao.getActiveLocationBasedTodos();
                Log.d(TAG, "Initializing geofences for " + activeLocationTodos.size() + " active location-based todos");

                if (!activeLocationTodos.isEmpty()) {
                    locationService.registerGeofences(activeLocationTodos);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing geofences", e);
            }
        });
    }

    // 위치 관련 메서드들
    public LiveData<List<LocationItem>> getAllLocations() {
        return allLocations;
    }

    public void insertLocation(LocationItem location) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                locationDao.insert(location);
                Log.d(TAG, "Inserted location: " + location.getName());
            } catch (Exception e) {
                Log.e(TAG, "Error inserting location", e);
            }
        });
    }

    public void updateLocation(LocationItem location) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                locationDao.update(location);
                Log.d(TAG, "Updated location: " + location.getName());

                // 해당 위치의 모든 할 일들에 대해 Geofence 업데이트
                List<TodoItem> locationTodos = todoDao.getTodosByLocationIdSync(location.getId());
                for (TodoItem todo : locationTodos) {
                    if (todo.isLocationEnabled() && !todo.isCompleted()) {
                        // 위치 정보 업데이트
                        todo.setLocationName(location.getName());
                        todo.setLocationLatitude(location.getLatitude());
                        todo.setLocationLongitude(location.getLongitude());
                        todo.setLocationRadius(location.getRadius());

                        // Geofence 재등록
                        locationService.removeGeofence(todo);
                        if (location.isEnabled()) {
                            locationService.registerGeofence(todo);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating location", e);
            }
        });
    }

    public void deleteLocation(LocationItem location) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 해당 위치의 모든 할 일들의 Geofence 제거
                List<TodoItem> locationTodos = todoDao.getTodosByLocationIdSync(location.getId());
                for (TodoItem todo : locationTodos) {
                    locationService.removeGeofence(todo);
                }

                locationDao.delete(location);
                Log.d(TAG, "Deleted location: " + location.getName());
            } catch (Exception e) {
                Log.e(TAG, "Error deleting location", e);
            }
        });
    }

    // 특정 위치의 할 일들 가져오기
    public LiveData<List<TodoItem>> getTodosByLocationId(int locationId) {
        return todoDao.getTodosByLocationId(locationId);
    }

    // 할 일 관련 메서드들
    public void insertTodo(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 위치 정보 가져오기 (동기 방식)
                LocationItem location = locationDao.getLocationByIdSync(todoItem.getLocationId());

                if (location != null) {
                    // TodoItem에 위치 정보(위도, 경도, 반경 등) 채우기
                    todoItem.setLocationName(location.getName());
                    todoItem.setLocationLatitude(location.getLatitude());
                    todoItem.setLocationLongitude(location.getLongitude());
                    todoItem.setLocationRadius(location.getRadius());

                    // 데이터베이스에 할 일을 삽입하고 ID 받기
                    long insertedId = todoDao.insertAndGetId(todoItem);
                    todoItem.setId((int) insertedId);

                    Log.d(TAG, "Inserted todo: " + todoItem.getTitle() + " with ID: " + insertedId);

                    // 위치 기능이 활성화되어 있고 위치도 활성화되어 있으면 Geofence 등록
                    if (todoItem.isLocationEnabled() && location.isEnabled()) {
                        Log.d(TAG, "Registering geofence for new todo: " + todoItem.getTitle());
                        locationService.registerGeofence(todoItem);
                    } else {
                        Log.d(TAG, "Geofence not registered - location enabled: " +
                                todoItem.isLocationEnabled() + ", location active: " + location.isEnabled());
                    }
                } else {
                    Log.w(TAG, "Location not found for ID: " + todoItem.getLocationId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting todo", e);
            }
        });
    }

    public void updateTodo(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                LocationItem location = locationDao.getLocationByIdSync(todoItem.getLocationId());
                if (location != null) {
                    todoItem.setLocationName(location.getName());
                    todoItem.setLocationLatitude(location.getLatitude());
                    todoItem.setLocationLongitude(location.getLongitude());
                    todoItem.setLocationRadius(location.getRadius());

                    todoDao.update(todoItem);
                    Log.d(TAG, "Updated todo: " + todoItem.getTitle());

                    // Geofence 업데이트 (기존 것 삭제 후 새로 등록)
                    locationService.removeGeofence(todoItem);

                    // 완료되지 않고 위치 기능이 활성화되어 있으며 위치도 활성화되어 있으면 재등록
                    if (!todoItem.isCompleted() && todoItem.isLocationEnabled() && location.isEnabled()) {
                        Log.d(TAG, "Re-registering geofence for updated todo: " + todoItem.getTitle());
                        locationService.registerGeofence(todoItem);
                    }
                } else {
                    Log.w(TAG, "Location not found for ID: " + todoItem.getLocationId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating todo", e);
            }
        });
    }

    public void deleteTodo(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                todoDao.delete(todoItem);
                Log.d(TAG, "Deleted todo: " + todoItem.getTitle());

                // 할 일이 삭제되면 Geofence도 해제
                locationService.removeGeofence(todoItem);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting todo", e);
            }
        });
    }
}