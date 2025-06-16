package com.am.mytodolistapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    void insert(CategoryItem categoryItem);

    @Insert
    void insertAll(List<CategoryItem> categories);

    @Update
    void update(CategoryItem categoryItem);

    @Delete
    void delete(CategoryItem categoryItem);

    // 모든 카테고리 조회
    @Query("SELECT * FROM category_table ORDER BY order_index ASC, created_at ASC")
    LiveData<List<CategoryItem>> getAllCategories();

    // 특정 ID의 카테고리 조회
    @Query("SELECT * FROM category_table WHERE id = :id")
    LiveData<CategoryItem> getCategoryById(int id);

    @Query("SELECT * FROM category_table WHERE id = :id")
    CategoryItem getCategoryByIdSync(int id);

    // 기본 카테고리만 조회
    @Query("SELECT * FROM category_table WHERE is_default = 1 ORDER BY order_index ASC")
    LiveData<List<CategoryItem>> getDefaultCategories();

    // 사용자 정의 카테고리만 조회
    @Query("SELECT * FROM category_table WHERE is_default = 0 ORDER BY created_at ASC")
    LiveData<List<CategoryItem>> getUserCategories();

    // 카테고리 이름으로 검색
    @Query("SELECT * FROM category_table WHERE name LIKE '%' || :name || '%'")
    LiveData<List<CategoryItem>> getCategoriesByName(String name);

    // 특정 카테고리를 사용하는 할 일의 개수 조회
    @Query("SELECT COUNT(*) FROM todo_table WHERE category_id = :categoryId")
    int getTodoCountByCategory(int categoryId);

    // 카테고리 순서 업데이트
    @Query("UPDATE category_table SET order_index = :orderIndex WHERE id = :id")
    void updateCategoryOrder(int id, int orderIndex);

    @Query("DELETE FROM category_table")
    void deleteAllCategories();

    // 기본 카테고리 존재 여부 확인
    @Query("SELECT COUNT(*) FROM category_table WHERE is_default = 1")
    int getDefaultCategoryCount();
}