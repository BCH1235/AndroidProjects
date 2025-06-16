package com.am.mytodolistapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

// 'location_table'에 접근하기 위한 데이터 접근 객체
@Dao
public interface LocationDao {

    @Insert
    void insert(LocationItem locationItem); // 새로운 위치 정보를 'location_table'에 삽입

    @Update
    void update(LocationItem locationItem); // 기존 위치 정보를 업데이트

    @Delete
    void delete(LocationItem locationItem); // 특정 위치 정보를 삭제

    @Query("SELECT * FROM location_table ORDER BY name ASC")
    LiveData<List<LocationItem>> getAllLocations(); // 저장된 모든 위치 정보를 이름 오름차순으로 조회

    @Query("SELECT * FROM location_table WHERE id = :id")
    LiveData<LocationItem> getLocationById(int id);

    @Query("SELECT * FROM location_table WHERE id = :id")
    LocationItem getLocationByIdSync(int id);

    @Query("DELETE FROM location_table")
    void deleteAllLocations();

    @Query("SELECT COUNT(*) FROM todo_table WHERE location_id = :locationId")
    int countTodosByLocationId(int locationId);
}