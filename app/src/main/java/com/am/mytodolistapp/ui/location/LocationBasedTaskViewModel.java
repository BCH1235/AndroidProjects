package com.am.mytodolistapp.ui.location;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
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

    // 위치 삭제 시 확인 다이얼로그를 위한 인터페이스
    public interface OnLocationDeleteListener {
        void onLocationDeleteConfirmed(LocationItem location, int todoCount);
        void onLocationDeleteCancelled();
    }

    private OnLocationDeleteListener deleteListener;

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

    // 위치 삭제 리스너 설정
    public void setOnLocationDeleteListener(OnLocationDeleteListener listener) {
        this.deleteListener = listener;
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

    // =============== 위치 관련 메서드들 ===============

    public LiveData<List<LocationItem>> getAllLocations() {
        return allLocations;
    }

    public void insertLocation(LocationItem location) {
        if (location == null || location.getName() == null || location.getName().trim().isEmpty()) {
            Log.w(TAG, "Cannot insert location: invalid location data");
            return;
        }

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
        if (location == null || location.getId() <= 0) {
            Log.w(TAG, "Cannot update location: invalid location data");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                locationDao.update(location);
                Log.d(TAG, "Updated location: " + location.getName());

                // 해당 위치의 모든 할 일들에 대해 Geofence 업데이트
                List<TodoItem> locationTodos = todoDao.getTodosByLocationIdSync(location.getId());
                for (TodoItem todo : locationTodos) {
                    if (todo.isLocationEnabled() && !todo.isCompleted()) {
                        // 위치 정보 업데이트
                        updateTodoLocationInfo(todo, location);

                        // Geofence 재등록
                        locationService.removeGeofence(todo);
                        if (location.isEnabled()) {
                            locationService.registerGeofence(todo);
                        }

                        // 할 일 업데이트
                        todoDao.update(todo);
                    }
                }
                Log.d(TAG, "Updated " + locationTodos.size() + " todos with new location info");
            } catch (Exception e) {
                Log.e(TAG, "Error updating location", e);
            }
        });
    }

    // 안전한 위치 삭제 (확인 다이얼로그 포함)
    public void deleteLocationSafely(LocationItem location) {
        if (location == null || location.getId() <= 0) {
            Log.w(TAG, "Cannot delete location: invalid location data");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 해당 위치의 할 일 개수 확인
                List<TodoItem> locationTodos = todoDao.getTodosByLocationIdSync(location.getId());
                int todoCount = locationTodos.size();

                // UI 스레드에서 확인 다이얼로그 표시
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (deleteListener != null) {
                        deleteListener.onLocationDeleteConfirmed(location, todoCount);
                    } else {
                        // 리스너가 설정되지 않은 경우 바로 삭제
                        if (todoCount > 0) {
                            Log.i(TAG, "Deleting location with " + todoCount + " associated todos");
                        }
                        deleteLocationWithTodos(location);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error checking location todos for safe deletion", e);
            }
        });
    }

    // 위치와 관련된 모든 할 일 삭제
    public void deleteLocationWithTodos(LocationItem location) {
        if (location == null || location.getId() <= 0) {
            Log.w(TAG, "Cannot delete location: invalid location data");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                //해당 위치의 할 일들의 Geofence 제거
                List<TodoItem> locationTodos = todoDao.getTodosByLocationIdSync(location.getId());
                for (TodoItem todo : locationTodos) {
                    locationService.removeGeofence(todo);
                }
                Log.d(TAG, "Removed " + locationTodos.size() + " geofences");

                //해당 위치의 모든 할 일들을 한 번에 삭제
                // todoDao.deleteAllTodosByLocationId(location.getId());

                // 또는 개별 삭제
                for (TodoItem todo : locationTodos) {
                    todoDao.delete(todo);
                }

                //위치 삭제
                locationDao.delete(location);

                Log.d(TAG, "Successfully deleted location: " + location.getName() +
                        " and " + locationTodos.size() + " associated todos");

            } catch (Exception e) {
                Log.e(TAG, "Error deleting location and associated todos", e);
            }
        });
    }

    // 단순 위치 삭제
    public void deleteLocation(LocationItem location) {
        deleteLocationWithTodos(location);
    }

    // =============== 할 일 관련 메서드들 ===============

    // 특정 위치의 할 일들 가져오기
    public LiveData<List<TodoItem>> getTodosByLocationId(int locationId) {
        return todoDao.getTodosByLocationId(locationId);
    }

    public void insertTodo(TodoItem todoItem) {
        if (todoItem == null || todoItem.getTitle() == null || todoItem.getTitle().trim().isEmpty()) {
            Log.w(TAG, "Cannot insert todo: invalid todo data");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 위치 정보 가져오기
                LocationItem location = locationDao.getLocationByIdSync(todoItem.getLocationId());

                if (location != null) {
                    // TodoItem에 위치 정보(위도, 경도, 반경 등) 채우기
                    updateTodoLocationInfo(todoItem, location);

                    // 시간 정보 설정
                    long currentTime = System.currentTimeMillis();
                    todoItem.setCreatedAt(currentTime);
                    todoItem.setUpdatedAt(currentTime);

                    // 데이터베이스에 할 일을 삽입하고 ID 받기
                    long insertedId = todoDao.insertAndGetId(todoItem);
                    todoItem.setId((int) insertedId);

                    Log.d(TAG, "Inserted todo: " + todoItem.getTitle() + " with ID: " + insertedId);

                    // 위치 기능이 활성화되어 있고 위치도 활성화되어 있으면 Geofence 등록
                    if (todoItem.isLocationEnabled() && location.isEnabled()) {
                        Log.d(TAG, "Registering geofence for new todo: " + todoItem.getTitle());
                        locationService.registerGeofence(todoItem);
                    } else {
                        Log.d(TAG, "Geofence not registered - todo location enabled: " +
                                todoItem.isLocationEnabled() + ", location active: " + location.isEnabled());
                    }
                } else {
                    Log.w(TAG, "Location not found for ID: " + todoItem.getLocationId());
                    // 위치 정보 없이도 할 일은 저장
                    long insertedId = todoDao.insertAndGetId(todoItem);
                    todoItem.setId((int) insertedId);
                    Log.d(TAG, "Inserted todo without location: " + todoItem.getTitle());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting todo", e);
            }
        });
    }

    public void updateTodo(int todoId, String newTitle) {
        if (todoId <= 0 || newTitle == null || newTitle.trim().isEmpty()) {
            Log.w(TAG, "Cannot update todo: invalid data");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                //ID를 사용해 DB에서 기존 TodoItem을 가져온다
                TodoItem itemToUpdate = todoDao.getTodoByIdSync(todoId);

                if (itemToUpdate != null) {
                    // 필요한 부분(제목)만 수정합니다.
                    itemToUpdate.setTitle(newTitle);


                    // 정된 전체 객체를 사용하여 DB를 업데이트
                    todoDao.update(itemToUpdate);
                    Log.d(TAG, "Updated todo: " + itemToUpdate.getTitle());

                } else {
                    Log.e(TAG, "TodoItem to update not found with id: " + todoId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating todo", e);
            }
        });
    }

    public void toggleTodoCompletion(TodoItem todoItem) {
        if (todoItem == null) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 완료 상태를 반전
            todoItem.setCompleted(!todoItem.isCompleted());
            // 업데이트 시간은 setCompleted 내부에서 갱신

            todoDao.update(todoItem);
            Log.d(TAG, "Toggled completion for todo: " + todoItem.getTitle());

            // Geofence 업데이트 로직
            locationService.removeGeofence(todoItem);
            if (!todoItem.isCompleted() && todoItem.isLocationEnabled()) {
                LocationItem location = locationDao.getLocationByIdSync(todoItem.getLocationId());
                if (location != null && location.isEnabled()) {
                    locationService.registerGeofence(todoItem);
                }
            }
        });
    }

    public void deleteTodo(TodoItem todoItem) {
        if (todoItem == null || todoItem.getId() <= 0) {
            Log.w(TAG, "Cannot delete todo: invalid todo data");
            return;
        }

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

    // =============== 유틸리티 메서드들 ===============

    // TodoItem에 위치 정보 업데이트
    private void updateTodoLocationInfo(TodoItem todoItem, LocationItem location) {
        if (todoItem != null && location != null) {
            todoItem.setLocationName(location.getName());
            todoItem.setLocationLatitude(location.getLatitude());
            todoItem.setLocationLongitude(location.getLongitude());
            todoItem.setLocationRadius(location.getRadius());
        }
    }

    // 특정 위치의 할 일 개수 조회
    public void getLocationTodoCount(int locationId, OnTodoCountResultListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<TodoItem> todos = todoDao.getTodosByLocationIdSync(locationId);
                int count = todos.size();

                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onResult(count);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting todo count for location", e);
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onResult(0);
                    });
                }
            }
        });
    }

    // 할 일 개수 결과를 받기 위한 인터페이스
    public interface OnTodoCountResultListener {
        void onResult(int count);
    }

    // Geofence 재초기화 (위치 서비스 재시작 시 사용)
    public void reinitializeGeofences() {
        Log.d(TAG, "Reinitializing all geofences...");
        initializeGeofences();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        deleteListener = null;
        Log.d(TAG, "LocationBasedTaskViewModel cleared");
    }
}