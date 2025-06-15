package com.am.mytodolistapp.ui;

import android.app.Application;

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
            // 위치 정보 가져오기 (동기 방식)
            LocationItem location = locationDao.getLocationByIdSync(todoItem.getLocationId());

            if (location != null) {
                // TodoItem에 위치 정보(위도, 경도, 반경 등) 채우기
                todoItem.setLocationName(location.getName());
                todoItem.setLocationLatitude(location.getLatitude());
                todoItem.setLocationLongitude(location.getLongitude());
                todoItem.setLocationRadius(location.getRadius());

                // 데이터베이스에 할 일을 삽입
                todoDao.insert(todoItem);


                //위치 정보가 채워진 TodoItem으로 지오펜스를 등록합니다.
                if (todoItem.isLocationEnabled()) {
                    locationService.registerGeofence(todoItem);
                }
            }
        });
    }

    public void updateTodo(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {

            LocationItem location = locationDao.getLocationByIdSync(todoItem.getLocationId());
            if (location != null) {
                todoItem.setLocationName(location.getName());
                todoItem.setLocationLatitude(location.getLatitude());
                todoItem.setLocationLongitude(location.getLongitude());
                todoItem.setLocationRadius(location.getRadius());

                todoDao.update(todoItem);

                // 지오펜스 업데이트 (기존 것 삭제 후 새로 등록)
                locationService.removeGeofence(todoItem);
                if (todoItem.isLocationEnabled()) {
                    locationService.registerGeofence(todoItem);
                }
            }
        });
    }

    public void deleteTodo(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.delete(todoItem);
            // 할 일이 삭제되면 지오펜스도 해제
            locationService.removeGeofence(todoItem);
        });
    }
}