package com.am.mytodolistapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao //Room DAO
public interface TodoDao {


    @Insert
    void insert(TodoItem todoItem); //삽입 메소드

    @Update
    void update(TodoItem todoItem); //수정 메소드

    @Delete
    void delete(TodoItem todoItem); //삭제 메소드

    // 모든 데이터 삭제
    @Query("DELETE FROM todo_table") //데이터를 삭제하는 SQL 쿼리
    void deleteAllTodos();

    //모든 데이터 조회
    // LiveData: 데이터가 변경되면 자동으로 UI에 알려줌
    @Query("SELECT * FROM todo_table ORDER BY id DESC") // todo_table 에서 모든 데이터를 id 내림차순 정렬해서 가져옴
    LiveData<List<TodoItem>> getAllTodos(); // 모든 할 일 목록을 LiveData 형태로 반환

    //특정 ID 데이터 조회
    @Query("SELECT * FROM todo_table WHERE id = :id") // 특정 id 와 일치하는 데이터만 가져옴
    LiveData<TodoItem> getTodoById(int id); // 특정 ID의 할 일을 LiveData 형태로 반환

    @Query("SELECT * FROM todo_table WHERE id = :id")
    TodoItem getTodoByIdSync(int id);//ID로 특정 할 일 직접 가져오기

    @Query("SELECT * FROM todo_table WHERE is_completed = 1 AND completion_timestamp >= :startTime AND completion_timestamp < :endTime ORDER BY completion_timestamp DESC")
    LiveData<List<TodoItem>> getCompletedTodosBetween(long startTime, long endTime);//특정 기간에 완료된 할 일 가져오기

    @Query("SELECT * FROM todo_table ORDER BY id DESC")
    List<TodoItem> getAllTodosSync();

    @Query("SELECT * FROM todo_table WHERE is_completed = 0 ORDER BY id DESC")
    List<TodoItem> getIncompleteTodosSync();


}