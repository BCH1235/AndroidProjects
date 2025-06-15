package com.am.mytodolistapp.ui.task;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.CategoryDao;
import com.am.mytodolistapp.data.CategoryItem;
import com.am.mytodolistapp.data.TodoDao;
import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.data.TodoRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TaskListViewModel extends AndroidViewModel {

    private TodoRepository mRepository;
    private TodoDao todoDao;
    private CategoryDao categoryDao;

    private final LiveData<List<TodoItem>> mAllTodos;
    private final LiveData<List<TodoWithCategory>> mAllTodosWithCategory;
    private final LiveData<List<CategoryItem>> mAllCategories;
    private final MediatorLiveData<List<TodoWithCategory>> mFilteredTodos;
    private int mCurrentCategoryFilter = -1; // -1: 전체, 0: 카테고리 없음, 양수: 특정 카테고리 ID

    // 월별 날짜별 완료율을 저장할 LiveData
    private final MutableLiveData<Map<LocalDate, Float>> monthlyCompletionRates = new MutableLiveData<>();

    //현재 표시 중인 월을 추적
    private YearMonth currentDisplayMonth = YearMonth.now();

    public TaskListViewModel(Application application) {
        super(application);
        mRepository = new TodoRepository(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        todoDao = db.todoDao();
        categoryDao = db.categoryDao();

        mAllTodos = mRepository.getAllTodos();
        mAllCategories = categoryDao.getAllCategories();

        mAllTodosWithCategory = Transformations.map(
                todoDao.getAllTodosWithCategory(),
                this::convertToTodoWithCategoryList
        );

        mFilteredTodos = new MediatorLiveData<>();
        mFilteredTodos.addSource(mAllTodosWithCategory, todos -> {
            applyCurrentFilter(todos);
            //할일 목록이 변경될 때마다 현재 표시 중인 월의 완료율 자동 업데이트
            updateMonthlyCompletionRates(currentDisplayMonth, todos);
        });
    }

    private List<TodoWithCategory> convertToTodoWithCategoryList(List<TodoDao.TodoWithCategoryInfo> infos) {
        List<TodoWithCategory> result = new ArrayList<>();
        if (infos != null) {
            for (TodoDao.TodoWithCategoryInfo info : infos) {
                result.add(new TodoWithCategory(
                        info.toTodoItem(),
                        info.category_name,
                        info.category_color
                ));
            }
        }
        return result;
    }

    private void applyCurrentFilter(List<TodoWithCategory> allTodos) {
        if (allTodos == null) {
            mFilteredTodos.setValue(new ArrayList<>());
            return;
        }

        List<TodoWithCategory> filteredList = new ArrayList<>();
        if (mCurrentCategoryFilter == -1) {
            filteredList.addAll(allTodos);
        } else if (mCurrentCategoryFilter == 0) {
            for (TodoWithCategory todo : allTodos) {
                if (todo.getTodoItem().getCategoryId() == null) {
                    filteredList.add(todo);
                }
            }
        } else {
            for (TodoWithCategory todo : allTodos) {
                if (todo.getTodoItem().getCategoryId() != null &&
                        Objects.equals(todo.getTodoItem().getCategoryId(), mCurrentCategoryFilter)) {
                    filteredList.add(todo);
                }
            }
        }
        mFilteredTodos.setValue(filteredList);
    }

    public LiveData<List<TodoItem>> getAllTodos() {
        return mAllTodos;
    }

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
        applyCurrentFilter(mAllTodosWithCategory.getValue());
    }

    public void showTodosWithoutCategory() {
        mCurrentCategoryFilter = 0;
        applyCurrentFilter(mAllTodosWithCategory.getValue());
    }

    public void showTodosByCategory(int categoryId) {
        mCurrentCategoryFilter = categoryId;
        applyCurrentFilter(mAllTodosWithCategory.getValue());
    }

    public int getCurrentCategoryFilter() {
        return mCurrentCategoryFilter;
    }

    //날짜별 완료율 계산 로직 수정
    public LiveData<Map<LocalDate, Float>> getMonthlyCompletionRates() {
        return monthlyCompletionRates;
    }

    //현재 표시 월 설정 메서드 추가
    public void setCurrentDisplayMonth(YearMonth yearMonth) {
        this.currentDisplayMonth = yearMonth;
        // 현재 표시 월이 변경되면 완료율 즉시 업데이트
        updateMonthlyCompletionRates(yearMonth, mAllTodosWithCategory.getValue());
    }

    public void updateMonthlyCompletionRates(YearMonth yearMonth, List<TodoWithCategory> allTasks) {
        if (allTasks == null) {
            monthlyCompletionRates.postValue(new HashMap<>());
            return;
        }

        this.currentDisplayMonth = yearMonth;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Map<LocalDate, Float> rates = new HashMap<>();
            LocalDate firstDayOfMonth = yearMonth.atDay(1);
            LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

            for (LocalDate date = firstDayOfMonth; !date.isAfter(lastDayOfMonth); date = date.plusDays(1)) {
                final LocalDate currentDate = date; // effectively final for lambda

                // 해당 날짜의 00:00:00 시각
                Calendar startOfDayCal = Calendar.getInstance();
                startOfDayCal.set(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth(), 0, 0, 0);
                startOfDayCal.set(Calendar.MILLISECOND, 0);
                long startOfDayMillis = startOfDayCal.getTimeInMillis();

                // 해당 날짜의 23:59:59 시각
                Calendar endOfDayCal = Calendar.getInstance();
                endOfDayCal.set(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth(), 23, 59, 59);
                endOfDayCal.set(Calendar.MILLISECOND, 999);
                long endOfDayMillis = endOfDayCal.getTimeInMillis();

                List<TodoItem> tasksForDate = allTasks.stream()
                        .map(TodoWithCategory::getTodoItem)
                        .filter(todo -> {
                            Long dueDate = todo.getDueDate();
                            // 기한이 없으면 오늘 날짜의 작업으로 간주
                            if (dueDate == null) {
                                return currentDate.equals(LocalDate.now());
                            }
                            return dueDate >= startOfDayMillis && dueDate <= endOfDayMillis;
                        })
                        .collect(Collectors.toList());

                if (!tasksForDate.isEmpty()) {
                    long completedCount = tasksForDate.stream().filter(TodoItem::isCompleted).count();
                    float rate = (float) completedCount / tasksForDate.size();
                    rates.put(currentDate, rate);
                } else {
                    rates.put(currentDate, 0f); // 할 일이 없으면 0%
                }
            }

            //스레드에서 값 업데이트
            monthlyCompletionRates.postValue(rates);
        });
    }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TodoWithCategory that = (TodoWithCategory) o;
            return Objects.equals(todoItem, that.todoItem) &&
                    Objects.equals(categoryName, that.categoryName) &&
                    Objects.equals(categoryColor, that.categoryColor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(todoItem, categoryName, categoryColor);
        }
    }
}