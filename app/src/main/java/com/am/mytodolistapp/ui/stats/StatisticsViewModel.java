package com.am.mytodolistapp.ui.stats;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.CategoryDao;
import com.am.mytodolistapp.data.CategoryItem;
import com.am.mytodolistapp.data.TodoDao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsViewModel extends AndroidViewModel {

    private TodoDao todoDao;
    private CategoryDao categoryDao;

    private LiveData<Integer> completedTasksCount;
    private LiveData<Integer> pendingTasksCount;
    private LiveData<List<DailyCompletionData>> dailyCompletionData;
    private LiveData<List<CategoryStatData>> incompleteByCategoryData;

    public StatisticsViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        todoDao = db.todoDao();
        categoryDao = db.categoryDao();

        initializeLiveData();
    }

    private void initializeLiveData() {
        // 완료된 작업 수
        completedTasksCount = Transformations.map(
                todoDao.getCompletedTodosWithCategory(),
                todos -> todos != null ? todos.size() : 0
        );

        // 보류중인 작업 수
        pendingTasksCount = Transformations.map(
                todoDao.getIncompleteTodosWithCategory(),
                todos -> todos != null ? todos.size() : 0
        );

        // 일일 완료 데이터 (일요일부터 토요일까지)
        dailyCompletionData = Transformations.map(
                todoDao.getCompletedTodosWithCategory(),
                this::calculateDailyCompletionData
        );

        // 카테고리별 미완료 작업 데이터
        MediatorLiveData<List<CategoryStatData>> categoryMediatorLiveData = new MediatorLiveData<>();
        LiveData<List<TodoDao.TodoWithCategoryInfo>> incompleteTodos = todoDao.getIncompleteTodosWithCategory();
        LiveData<List<CategoryItem>> allCategories = categoryDao.getAllCategories();

        categoryMediatorLiveData.addSource(incompleteTodos, todos -> {
            List<CategoryItem> categories = allCategories.getValue();
            if (categories != null) {
                categoryMediatorLiveData.setValue(calculateCategoryStatData(todos, categories));
            }
        });

        categoryMediatorLiveData.addSource(allCategories, categories -> {
            List<TodoDao.TodoWithCategoryInfo> todos = incompleteTodos.getValue();
            if (todos != null) {
                categoryMediatorLiveData.setValue(calculateCategoryStatData(todos, categories));
            }
        });

        incompleteByCategoryData = categoryMediatorLiveData;
    }

    private List<DailyCompletionData> calculateDailyCompletionData(List<TodoDao.TodoWithCategoryInfo> completedTodos) {
        List<DailyCompletionData> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 이번 주 일요일 찾기
        LocalDate thisWeekSunday = today;
        while (thisWeekSunday.getDayOfWeek().getValue() != 7) { // 7 = 일요일
            thisWeekSunday = thisWeekSunday.minusDays(1);
        }

        // 일요일부터 토요일까지 7일간의 데이터 초기화
        Map<LocalDate, Integer> dailyCount = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = thisWeekSunday.plusDays(i);
            dailyCount.put(date, 0);
        }

        // 완료된 작업들의 완료 날짜별로 카운트
        if (completedTodos != null) {
            for (TodoDao.TodoWithCategoryInfo todo : completedTodos) {
                LocalDate completionDate = LocalDate.ofEpochDay(todo.updated_at / (24 * 60 * 60 * 1000));
                if (dailyCount.containsKey(completionDate)) {
                    dailyCount.put(completionDate, dailyCount.get(completionDate) + 1);
                }
            }
        }

        // 결과 리스트 생성 (일요일부터 토요일 순서)
        for (int i = 0; i < 7; i++) {
            LocalDate date = thisWeekSunday.plusDays(i);
            String dayOfWeek = getDayOfWeekKorean(date.getDayOfWeek().getValue());
            result.add(new DailyCompletionData(dayOfWeek, dailyCount.get(date)));
        }

        return result;
    }

    private List<CategoryStatData> calculateCategoryStatData(List<TodoDao.TodoWithCategoryInfo> incompleteTodos,
                                                             List<CategoryItem> categories) {
        List<CategoryStatData> result = new ArrayList<>();
        Map<Integer, Integer> categoryCount = new HashMap<>();
        int noCategoryCount = 0;

        // 카테고리별 미완료 작업 수 계산
        if (incompleteTodos != null) {
            for (TodoDao.TodoWithCategoryInfo todo : incompleteTodos) {
                if (todo.category_id == null) {
                    noCategoryCount++;
                } else {
                    categoryCount.put(todo.category_id,
                            categoryCount.getOrDefault(todo.category_id, 0) + 1);
                }
            }
        }

        // 카테고리별 데이터 생성
        if (categories != null) {
            for (CategoryItem category : categories) {
                int count = categoryCount.getOrDefault(category.getId(), 0);
                if (count > 0) {
                    result.add(new CategoryStatData(category.getName(), count, category.getColor()));
                }
            }
        }

        // 카테고리 없는 작업이 있다면 추가
        if (noCategoryCount > 0) {
            result.add(new CategoryStatData("카테고리 없음", noCategoryCount, "#CCCCCC"));
        }

        return result;
    }

    private String getDayOfWeekKorean(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return "월";
            case 2: return "화";
            case 3: return "수";
            case 4: return "목";
            case 5: return "금";
            case 6: return "토";
            case 7: return "일";
            default: return "";
        }
    }

    // Getter 메서드들
    public LiveData<Integer> getCompletedTasksCount() {
        return completedTasksCount;
    }

    public LiveData<Integer> getPendingTasksCount() {
        return pendingTasksCount;
    }

    public LiveData<List<DailyCompletionData>> getDailyCompletionData() {
        return dailyCompletionData;
    }

    public LiveData<List<CategoryStatData>> getIncompleteByCategoryData() {
        return incompleteByCategoryData;
    }

    // 데이터 클래스들
    public static class DailyCompletionData {
        private final String dayOfWeek;
        private final int completedCount;

        public DailyCompletionData(String dayOfWeek, int completedCount) {
            this.dayOfWeek = dayOfWeek;
            this.completedCount = completedCount;
        }

        public String getDayOfWeek() { return dayOfWeek; }
        public int getCompletedCount() { return completedCount; }
    }

    public static class CategoryStatData {
        private final String categoryName;
        private final int count;
        private final String color;

        public CategoryStatData(String categoryName, int count, String color) {
            this.categoryName = categoryName;
            this.count = count;
            this.color = color;
        }

        public String getCategoryName() { return categoryName; }
        public int getCount() { return count; }
        public String getColor() { return color; }
    }
}