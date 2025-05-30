package com.am.mytodolistapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.CategoryDao;
import com.am.mytodolistapp.data.CategoryItem;
import com.am.mytodolistapp.data.TodoDao;
import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.data.TodoRepository;

import java.util.ArrayList;
import java.util.List;

public class TaskListViewModel extends AndroidViewModel {

    private TodoRepository mRepository;
    private TodoDao todoDao;
    private CategoryDao categoryDao;

    // 기존 LiveData 유지 (호환성)
    private final LiveData<List<TodoItem>> mAllTodos;

    // 카테고리 정보와 함께 제공되는 할 일 목록
    private final LiveData<List<TodoWithCategory>> mAllTodosWithCategory;
    private final LiveData<List<CategoryItem>> mAllCategories;

    // 필터링된 할 일 목록
    private LiveData<List<TodoWithCategory>> mFilteredTodos;
    private int mCurrentCategoryFilter = -1; // -1: 전체, 0: 카테고리 없음, 양수: 특정 카테고리 ID

    public TaskListViewModel(Application application) {
        super(application);
        mRepository = new TodoRepository(application);

        // 기존 방식 유지
        mAllTodos = mRepository.getAllTodos();

        // 직접 DAO 접근 (JOIN 쿼리 사용을 위해)
        AppDatabase db = AppDatabase.getDatabase(application);
        todoDao = db.todoDao();
        categoryDao = db.categoryDao();

        // 카테고리 정보와 함께 할 일 목록 로드
        mAllTodosWithCategory = Transformations.map(
                todoDao.getAllTodosWithCategory(),
                todoWithCategoryInfos -> {
                    List<TodoWithCategory> result = new ArrayList<>();
                    for (TodoDao.TodoWithCategoryInfo info : todoWithCategoryInfos) {
                        result.add(new TodoWithCategory(
                                info.toTodoItem(),
                                info.category_name,
                                info.category_color
                        ));
                    }
                    return result;
                }
        );

        mAllCategories = categoryDao.getAllCategories();
        mFilteredTodos = mAllTodosWithCategory; // 초기값은 전체 목록
    }

    // 기존 메서드들 (호환성 유지)
    public LiveData<List<TodoItem>> getAllTodos() {
        return mAllTodos;
    }

    // 새로 추가된 메서드들
    public LiveData<List<TodoWithCategory>> getAllTodosWithCategory() {
        return mFilteredTodos;
    }

    public LiveData<List<CategoryItem>> getAllCategories() {
        return mAllCategories;
    }

    public void insert(TodoItem todoItem) {
        mRepository.insert(todoItem);
    }

    public void update(TodoItem todoItem) {
        mRepository.update(todoItem);
    }

    public void delete(TodoItem todoItem) {
        mRepository.delete(todoItem);
    }

    public void deleteAllTodos() {
        mRepository.deleteAllTodos();
    }



    public void showAllTodos() {
        mCurrentCategoryFilter = -1;
        mFilteredTodos = mAllTodosWithCategory;
    }

    public void showTodosWithoutCategory() {
        mCurrentCategoryFilter = 0;
        mFilteredTodos = Transformations.map(
                todoDao.getTodosWithoutCategoryWithInfo(),
                todoWithCategoryInfos -> {
                    List<TodoWithCategory> result = new ArrayList<>();
                    for (TodoDao.TodoWithCategoryInfo info : todoWithCategoryInfos) {
                        result.add(new TodoWithCategory(
                                info.toTodoItem(),
                                info.category_name,
                                info.category_color
                        ));
                    }
                    return result;
                }
        );
    }

    public void showTodosByCategory(int categoryId) {
        mCurrentCategoryFilter = categoryId;
        mFilteredTodos = Transformations.map(
                todoDao.getTodosByCategoryWithInfo(categoryId),
                todoWithCategoryInfos -> {
                    List<TodoWithCategory> result = new ArrayList<>();
                    for (TodoDao.TodoWithCategoryInfo info : todoWithCategoryInfos) {
                        result.add(new TodoWithCategory(
                                info.toTodoItem(),
                                info.category_name,
                                info.category_color
                        ));
                    }
                    return result;
                }
        );
    }

    // 현재 필터 상태 반환
    public int getCurrentCategoryFilter() {
        return mCurrentCategoryFilter;
    }

    // TodoItem과 CategoryItem을 함께 담는 데이터 클래스
    public static class TodoWithCategory {
        private final TodoItem todoItem;
        private final String categoryName;
        private final String categoryColor;

        public TodoWithCategory(TodoItem todoItem, String categoryName, String categoryColor) {
            this.todoItem = todoItem;
            this.categoryName = categoryName;
            this.categoryColor = categoryColor;
        }

        public TodoItem getTodoItem() { return todoItem; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryColor() { return categoryColor; }
    }
}