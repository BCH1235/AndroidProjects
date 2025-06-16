package com.am.mytodolistapp.ui.category;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.CategoryDao;
import com.am.mytodolistapp.data.CategoryItem;
import com.am.mytodolistapp.data.TodoDao;

import java.util.Arrays;
import java.util.List;

//카테고리 관련 데이터와 로직을 관리하는 ViewModel
// UI(Fragment, Dialog)와 데이터 소스(Repository, DAO) 사이의 중개자 역할
public class CategoryViewModel extends AndroidViewModel {

    private CategoryDao categoryDao;
    private TodoDao todoDao; // 카테고리 삭제 시 관련 할 일 개수를 확인하기 위해 필요
    private LiveData<List<CategoryItem>> allCategories; // 모든 카테고리 목록을 관찰 가능한 LiveData

    public CategoryViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        categoryDao = db.categoryDao();
        todoDao = db.todoDao();
        allCategories = categoryDao.getAllCategories();

        // 앱 첫 실행 시 기본 카테고리 생성
        initializeDefaultCategories();
    }

    // 기본 카테고리 초기화
    private void initializeDefaultCategories() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 기본 카테고리가 이미 존재하는지 확인
            if (categoryDao.getDefaultCategoryCount() == 0) {
                List<CategoryItem> defaultCategories = Arrays.asList(
                        CategoryItem.createDefaultCategory("업무", "#FF4444", 1),      // 빨간색
                        CategoryItem.createDefaultCategory("개인", "#FF8800", 2),      // 주황색
                        CategoryItem.createDefaultCategory("쇼핑", "#FFDD00", 3),      // 노란색
                        CategoryItem.createDefaultCategory("건강", "#44AA44", 4),      // 초록색
                        CategoryItem.createDefaultCategory("학습", "#4488FF", 5)       // 파란색
                );
                categoryDao.insertAll(defaultCategories);
            }
        });
    }

    // 카테고리 관련 메서드들
    public LiveData<List<CategoryItem>> getAllCategories() {
        return allCategories;
    }

    public LiveData<List<CategoryItem>> getDefaultCategories() {
        return categoryDao.getDefaultCategories();
    }

    public LiveData<List<CategoryItem>> getUserCategories() {
        return categoryDao.getUserCategories();
    }

    public void insertCategory(CategoryItem category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.insert(category);
        });
    }

    public void updateCategory(CategoryItem category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.update(category);
        });
    }

    public void deleteCategory(CategoryItem category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.delete(category);
        });
    }

    // 카테고리 삭제 시 해당 카테고리를 사용하는 할 일의 개수 확인
    public void deleteCategoryWithCheck(CategoryItem category, DeleteCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int todoCount = categoryDao.getTodoCountByCategory(category.getId());
            callback.onResult(todoCount);

            if (todoCount == 0) {
                categoryDao.delete(category);
            }
        });
    }

    // 카테고리 순서 변경
    public void updateCategoryOrder(int categoryId, int newOrder) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.updateCategoryOrder(categoryId, newOrder);
        });
    }

    // 카테고리별 할 일 개수 조회
    public void getTodoCountByCategory(int categoryId, CountCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = categoryDao.getTodoCountByCategory(categoryId);
            callback.onCount(count);
        });
    }

    // 콜백 인터페이스들
    public interface DeleteCallback {
        void onResult(int todoCount);
    }

    public interface CountCallback {
        void onCount(int count);
    }

    // 무지개 색상 ***
    public static final String[] PREDEFINED_COLORS = {
            "#FF4444", // 빨간색
            "#FF8800", // 주황색
            "#FFDD00", // 노란색
            "#44AA44", // 초록색
            "#4488FF", // 파란색
            "#6644AA", // 남색
            "#AA44AA"  // 보라색
    };

    // 랜덤 색상 가져오기
    public static String getRandomColor() {
        int randomIndex = (int) (Math.random() * PREDEFINED_COLORS.length);
        return PREDEFINED_COLORS[randomIndex];
    }
}