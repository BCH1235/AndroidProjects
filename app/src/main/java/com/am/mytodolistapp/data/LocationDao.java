package com.am.mytodolistapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocationDao {

    @Insert
    void insert(LocationItem locationItem);

    @Update
    void update(LocationItem locationItem);

    @Delete
    void delete(LocationItem locationItem);

    @Query("SELECT * FROM location_table ORDER BY name ASC")
    LiveData<List<LocationItem>> getAllLocations();

    @Query("SELECT * FROM location_table WHERE id = :id")
    LiveData<LocationItem> getLocationById(int id);

    @Query("SELECT * FROM location_table WHERE id = :id")
    LocationItem getLocationByIdSync(int id);

    @Query("DELETE FROM location_table")
    void deleteAllLocations();

    @Query("SELECT COUNT(*) FROM todo_table WHERE location_id = :locationId")
    int countTodosByLocationId(int locationId);
}