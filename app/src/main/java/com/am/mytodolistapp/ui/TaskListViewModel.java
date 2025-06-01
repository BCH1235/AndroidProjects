package com.am.mytodolistapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
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

    // 원본 데이터 (필터링되지 않음)
    private final LiveData<List<TodoWithCategory>> mAllTodosWithCategory;
    private final LiveData<List<CategoryItem>> mAllCategories;

    // 필터링된 할일 목록을 위한 MediatorLiveData
    private final MediatorLiveData<List<TodoWithCategory>> mFilteredTodos;
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

        // MediatorLiveData를 사용하여 필터링 구현
        mFilteredTodos = new MediatorLiveData<>();
        mFilteredTodos.addSource(mAllTodosWithCategory, todos -> {
            applyCurrentFilter(todos);
        });
    }

    // 현재 필터를 적용하는 메서드
    private void applyCurrentFilter(List<TodoWithCategory> allTodos) {
        if (allTodos == null) {
            mFilteredTodos.setValue(new ArrayList<>());
            return;
        }

        List<TodoWithCategory> filteredList = new ArrayList<>();

        if (mCurrentCategoryFilter == -1) {
            // 전체 보기
            filteredList.addAll(allTodos);
        } else if (mCurrentCategoryFilter == 0) {
            // 카테고리 없는 할일만
            for (TodoWithCategory todo : allTodos) {
                if (todo.getTodoItem().getCategoryId() == null) {
                    filteredList.add(todo);
                }
            }
        } else {
            // 특정 카테고리의 할일만
            for (TodoWithCategory todo : allTodos) {
                if (todo.getTodoItem().getCategoryId() != null &&
                        todo.getTodoItem().getCategoryId() == mCurrentCategoryFilter) {
                    filteredList.add(todo);
                }
            }
        }

        mFilteredTodos.setValue(filteredList);
    }

    // 기존 메서드들 (호환성 유지)
    public LiveData<List<TodoItem>> getAllTodos() {
        return mAllTodos;
    }

    // 필터링된 할일 목록 반환
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

    // 필터링 메서드들
    public void showAllTodos() {
        mCurrentCategoryFilter = -1;
        // 현재 데이터에 필터 재적용
        List<TodoWithCategory> currentData = mAllTodosWithCategory.getValue();
        applyCurrentFilter(currentData);
    }

    public void showTodosWithoutCategory() {
        mCurrentCategoryFilter = 0;
        List<TodoWithCategory> currentData = mAllTodosWithCategory.getValue();
        applyCurrentFilter(currentData);
    }

    public void showTodosByCategory(int categoryId) {
        mCurrentCategoryFilter = categoryId;
        List<TodoWithCategory> currentData = mAllTodosWithCategory.getValue();
        applyCurrentFilter(currentData);
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