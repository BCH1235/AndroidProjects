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

    // 🆕 필터링 옵션 추가
    private boolean mShowCollaborationTodos = true; // 협업 할 일 표시 여부
    private boolean mShowLocalTodos = true; // 로컬 할 일 표시 여부

    // 월별 날짜별 완료율을 저장할 LiveData
    private final MutableLiveData<Map<LocalDate, Float>> monthlyCompletionRates = new MutableLiveData<>();
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
            updateMonthlyCompletionRates(currentDisplayMonth, todos);
        });

        // 🆕 앱 시작 시 협업 동기화 시작
        mRepository.startCollaborationSync();
    }

    private List<TodoWithCategory> convertToTodoWithCategoryList(List<TodoDao.TodoWithCategoryInfo> infos) {
        List<TodoWithCategory> result = new ArrayList<>();
        if (infos != null) {
            for (TodoDao.TodoWithCategoryInfo info : infos) {
                TodoItem todoItem = info.toTodoItem();

                // 🆕 협업 할 일의 제목에 프로젝트 정보 추가
                String displayTitle = todoItem.isFromCollaboration() && todoItem.getProjectName() != null
                        ? "[" + todoItem.getProjectName() + "] " + todoItem.getTitle()
                        : todoItem.getTitle();

                result.add(new TodoWithCategory(
                        todoItem,
                        info.category_name,
                        info.category_color,
                        displayTitle // 🆕 표시용 제목 추가
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

        for (TodoWithCategory todo : allTodos) {
            TodoItem todoItem = todo.getTodoItem();

            // 🆕 협업/로컬 필터링 적용
            if (todoItem.isFromCollaboration() && !mShowCollaborationTodos) {
                continue; // 협업 할 일 숨김
            }
            if (!todoItem.isFromCollaboration() && !mShowLocalTodos) {
                continue; // 로컬 할 일 숨김
            }

            // 기존 카테고리 필터링 적용
            if (mCurrentCategoryFilter == -1) {
                filteredList.add(todo);
            } else if (mCurrentCategoryFilter == 0) {
                if (todoItem.getCategoryId() == null) {
                    filteredList.add(todo);
                }
            } else {
                if (todoItem.getCategoryId() != null &&
                        Objects.equals(todoItem.getCategoryId(), mCurrentCategoryFilter)) {
                    filteredList.add(todo);
                }
            }
        }

        mFilteredTodos.setValue(filteredList);
    }

    // ========== 기존 메서드들 (일부 수정) ==========

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

    public void update(TodoItem updatedItem) {
        // 🆕 협업 할 일과 로컬 할 일을 구분하여 처리
        if (updatedItem.isFromCollaboration()) {
            mRepository.toggleCollaborationTodoCompletion(updatedItem);
        } else {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                TodoItem itemToUpdate = todoDao.getTodoByIdSync(updatedItem.getId());
                if (itemToUpdate != null) {
                    itemToUpdate.setTitle(updatedItem.getTitle());
                    itemToUpdate.setCompleted(updatedItem.isCompleted());
                    itemToUpdate.setCategoryId(updatedItem.getCategoryId());
                    itemToUpdate.setDueDate(updatedItem.getDueDate());
                    mRepository.update(itemToUpdate);
                }
            });
        }
    }

    public void toggleCompletion(TodoItem todoItem) {
        // 🆕 협업 할 일과 로컬 할 일을 구분하여 처리
        if (todoItem.isFromCollaboration()) {
            mRepository.toggleCollaborationTodoCompletion(todoItem);
        } else {
            // 기존 로컬 할 일 토글 로직
            todoItem.setCompleted(!todoItem.isCompleted());
            List<TodoWithCategory> currentList = mFilteredTodos.getValue();
            if (currentList != null) {
                mFilteredTodos.setValue(new ArrayList<>(currentList));
            }

            AppDatabase.databaseWriteExecutor.execute(() -> {
                TodoItem itemToUpdate = todoDao.getTodoByIdSync(todoItem.getId());
                if (itemToUpdate != null) {
                    itemToUpdate.setCompleted(todoItem.isCompleted());
                    mRepository.update(itemToUpdate);
                }
            });
        }
    }

    public void delete(TodoItem todoItem) {
        mRepository.delete(todoItem);
    }

    public void deleteAllTodos() {
        mRepository.deleteAllTodos();
    }

    // ========== 🆕 새로운 필터링 메서드들 ==========

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

    /**
     * 🆕 협업 할 일 표시/숨김 토글
     */
    public void toggleCollaborationTodosVisibility() {
        mShowCollaborationTodos = !mShowCollaborationTodos;
        applyCurrentFilter(mAllTodosWithCategory.getValue());
    }

    /**
     * 🆕 로컬 할 일 표시/숨김 토글
     */
    public void toggleLocalTodosVisibility() {
        mShowLocalTodos = !mShowLocalTodos;
        applyCurrentFilter(mAllTodosWithCategory.getValue());
    }

    /**
     * 🆕 협업 할 일만 표시
     */
    public void showOnlyCollaborationTodos() {
        mShowCollaborationTodos = true;
        mShowLocalTodos = false;
        applyCurrentFilter(mAllTodosWithCategory.getValue());
    }

    /**
     * 🆕 로컬 할 일만 표시
     */
    public void showOnlyLocalTodos() {
        mShowCollaborationTodos = false;
        mShowLocalTodos = true;
        applyCurrentFilter(mAllTodosWithCategory.getValue());
    }

    /**
     * 🆕 모든 타입의 할 일 표시
     */
    public void showAllTypes() {
        mShowCollaborationTodos = true;
        mShowLocalTodos = true;
        applyCurrentFilter(mAllTodosWithCategory.getValue());
    }

    /**
     * 🆕 수동 동기화 수행
     */
    public void performManualSync() {
        mRepository.performManualSync();
    }

    // ========== Getters for current state ==========

    public int getCurrentCategoryFilter() {
        return mCurrentCategoryFilter;
    }

    public boolean isShowingCollaborationTodos() {
        return mShowCollaborationTodos;
    }

    public boolean isShowingLocalTodos() {
        return mShowLocalTodos;
    }

    // ========== 월별 완료율 관련 (기존 유지) ==========

    public LiveData<Map<LocalDate, Float>> getMonthlyCompletionRates() {
        return monthlyCompletionRates;
    }

    public void setCurrentDisplayMonth(YearMonth yearMonth) {
        this.currentDisplayMonth = yearMonth;
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
                final LocalDate currentDate = date;

                Calendar startOfDayCal = Calendar.getInstance();
                startOfDayCal.set(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth(), 0, 0, 0);
                startOfDayCal.set(Calendar.MILLISECOND, 0);
                long startOfDayMillis = startOfDayCal.getTimeInMillis();

                Calendar endOfDayCal = Calendar.getInstance();
                endOfDayCal.set(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth(), 23, 59, 59);
                endOfDayCal.set(Calendar.MILLISECOND, 999);
                long endOfDayMillis = endOfDayCal.getTimeInMillis();

                List<TodoItem> tasksForDate = allTasks.stream()
                        .map(TodoWithCategory::getTodoItem)
                        .filter(todo -> {
                            Long dueDate = todo.getDueDate();
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
                    rates.put(currentDate, 0f);
                }
            }

            monthlyCompletionRates.postValue(rates);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 🆕 ViewModel이 클리어될 때 동기화 중지
        mRepository.stopCollaborationSync();
    }

    // ========== 🆕 수정된 TodoWithCategory 클래스 ==========

    public static class TodoWithCategory {
        private final TodoItem todoItem;
        private final String categoryName;
        private final String categoryColor;
        private final String displayTitle; // 🆕 표시용 제목

        public TodoWithCategory(TodoItem todoItem, String categoryName, String categoryColor) {
            this(todoItem, categoryName, categoryColor, todoItem.getTitle());
        }

        public TodoWithCategory(TodoItem todoItem, String categoryName, String categoryColor, String displayTitle) {
            this.todoItem = todoItem;
            this.categoryName = categoryName;
            this.categoryColor = categoryColor;
            this.displayTitle = displayTitle;
        }

        public TodoItem getTodoItem() { return todoItem; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryColor() { return categoryColor; }
        public String getDisplayTitle() { return displayTitle; } // 🆕

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TodoWithCategory that = (TodoWithCategory) o;
            return Objects.equals(todoItem, that.todoItem) &&
                    Objects.equals(categoryName, that.categoryName) &&
                    Objects.equals(categoryColor, that.categoryColor) &&
                    Objects.equals(displayTitle, that.displayTitle);
        }

        @Override
        public int hashCode() {
            return Objects.hash(todoItem, categoryName, categoryColor, displayTitle);
        }
    }
}