package com.am.mytodolistapp.ui.task;

import android.app.Application;
import android.util.Log;

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
    private static final String TAG = "TaskListViewModel";

    private TodoRepository mRepository;
    private TodoDao todoDao;
    private CategoryDao categoryDao;

    private final LiveData<List<TodoItem>> mAllTodos; // 보관되지 않은 할 일 (기본)
    private final LiveData<List<TodoWithCategory>> mVisibleTodosWithCategory; // 화면에 보여줄 보관되지 않은 할 일
    private final LiveData<List<TodoWithCategory>> mAllTodosForStats; // 통계용 모든 할 일 (보관된 항목 포함)
    private final LiveData<List<CategoryItem>> mAllCategories;
    private final MediatorLiveData<List<TodoWithCategory>> mFilteredTodos;

    // 필터링 상태
    private int mCurrentCategoryFilter = -1; // -1: 전체, 0: 카테고리 없음, 양수: 특정 카테고리 ID
    private boolean mShowCollaborationTodos = true; // 협업 할 일 표시 여부
    private boolean mShowLocalTodos = true; // 로컬 할 일 표시 여부

    // 월별 날짜별 완료율을 저장할 LiveData
    private final MutableLiveData<Map<LocalDate, Float>> monthlyCompletionRates = new MutableLiveData<>();
    private YearMonth currentDisplayMonth = YearMonth.now();

    private final MutableLiveData<Boolean> isSyncActive = new MutableLiveData<>();
    private final MutableLiveData<String> syncStatusMessage = new MutableLiveData<>();

    public TaskListViewModel(Application application) {
        super(application);
        mRepository = new TodoRepository(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        todoDao = db.todoDao();
        categoryDao = db.categoryDao();

        archiveOldTodos();

        mAllTodos = mRepository.getAllTodos();
        mAllCategories = categoryDao.getAllCategories();

        // 1. 화면에 보여줄, 보관되지 않은 할 일 목록
        mVisibleTodosWithCategory = Transformations.map(
                todoDao.getAllTodosWithCategory(), // is_archived = 0 쿼리 사용
                this::convertToTodoWithCategoryList
        );

        // 2. 통계용, 모든 할 일 목록 (완료/보관 포함)
        MediatorLiveData<List<TodoWithCategory>> allTodosMediator = new MediatorLiveData<>();
        LiveData<List<TodoDao.TodoWithCategoryInfo>> completed = todoDao.getCompletedTodosWithCategory();
        LiveData<List<TodoDao.TodoWithCategoryInfo>> incomplete = todoDao.getIncompleteTodosWithCategory();

        allTodosMediator.addSource(completed, completedList -> {
            List<TodoDao.TodoWithCategoryInfo> incompleteList = incomplete.getValue();
            if (incompleteList != null) {
                List<TodoDao.TodoWithCategoryInfo> combined = new ArrayList<>();
                if (completedList != null) combined.addAll(completedList);
                combined.addAll(incompleteList);
                allTodosMediator.setValue(convertToTodoWithCategoryList(combined));
            }
        });
        allTodosMediator.addSource(incomplete, incompleteList -> {
            List<TodoDao.TodoWithCategoryInfo> completedList = completed.getValue();
            if (completedList != null) {
                List<TodoDao.TodoWithCategoryInfo> combined = new ArrayList<>();
                combined.addAll(completedList);
                if (incompleteList != null) combined.addAll(incompleteList);
                allTodosMediator.setValue(convertToTodoWithCategoryList(combined));
            }
        });
        mAllTodosForStats = allTodosMediator;

        // 3. 최종 필터링된 목록 (화면 표시용)
        mFilteredTodos = new MediatorLiveData<>();
        mFilteredTodos.addSource(mVisibleTodosWithCategory, todos -> {
            applyCurrentFilter(todos);
        });

        // 4. 캘린더 완료율 계산은 모든 데이터를 기준으로 관찰
        mAllTodosForStats.observeForever(allTasks -> {
            updateMonthlyCompletionRates(currentDisplayMonth, allTasks);
        });

        initializeCollaborationSync();
        Log.d(TAG, "TaskListViewModel initialized");
    }

    private void archiveOldTodos() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long todayTimestamp = calendar.getTimeInMillis();
            todoDao.archiveOldCompletedTodos(todayTimestamp);
            Log.d(TAG, "Archived old completed todos.");
        });
    }

    private void initializeCollaborationSync() {
        try {
            mRepository.startCollaborationSync();
            isSyncActive.setValue(true);
            syncStatusMessage.setValue("동기화 활성화됨");
            Log.d(TAG, "Collaboration sync initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize collaboration sync", e);
            isSyncActive.setValue(false);
            syncStatusMessage.setValue("동기화 초기화 실패");
        }
    }

    private List<TodoWithCategory> convertToTodoWithCategoryList(List<TodoDao.TodoWithCategoryInfo> infos) {
        List<TodoWithCategory> result = new ArrayList<>();
        if (infos != null) {
            for (TodoDao.TodoWithCategoryInfo info : infos) {
                TodoItem todoItem = info.toTodoItem();
                String displayTitle = todoItem.getDisplayTitle();
                result.add(new TodoWithCategory(
                        todoItem,
                        info.category_name,
                        info.category_color,
                        displayTitle
                ));
            }
        }
        return result;
    }

    private void applyCurrentFilter(List<TodoWithCategory> visibleTodos) {
        if (visibleTodos == null) {
            mFilteredTodos.setValue(new ArrayList<>());
            return;
        }

        List<TodoWithCategory> filteredList = new ArrayList<>();

        for (TodoWithCategory todo : visibleTodos) {
            TodoItem todoItem = todo.getTodoItem();

            if (todoItem.isFromCollaboration() && !mShowCollaborationTodos) {
                continue;
            }
            if (!todoItem.isFromCollaboration() && !mShowLocalTodos) {
                continue;
            }

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
        Log.d(TAG, "Filtered todos: " + filteredList.size() + " items");
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
        Log.d(TAG, "Inserting new todo: " + todoItem.getTitle());
        mRepository.insert(todoItem);
    }

    public void update(TodoItem updatedItem) {
        Log.d(TAG, "Updating todo: " + updatedItem.getTitle() + " (collaboration: " + updatedItem.isFromCollaboration() + ")");

        if (updatedItem.isFromCollaboration()) {
            mRepository.update(updatedItem);
        } else {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                TodoItem itemToUpdate = todoDao.getTodoByIdSync(updatedItem.getId());
                if (itemToUpdate != null) {
                    itemToUpdate.setTitle(updatedItem.getTitle());
                    itemToUpdate.setCompleted(updatedItem.isCompleted());
                    itemToUpdate.setCategoryId(updatedItem.getCategoryId());
                    itemToUpdate.setDueDate(updatedItem.getDueDate());
                    mRepository.update(itemToUpdate);
                    Log.d(TAG, "Local todo updated successfully");
                }
            });
        }
    }

    public void toggleCompletion(TodoItem todoItem) {
        Log.d(TAG, "Toggling completion for: " + todoItem.getTitle() + " (collaboration: " + todoItem.isFromCollaboration() + ")");

        boolean newCompletionState = !todoItem.isCompleted();
        todoItem.setCompleted(newCompletionState);

        List<TodoWithCategory> currentList = mFilteredTodos.getValue();
        if (currentList != null) {
            List<TodoWithCategory> updatedList = new ArrayList<>();
            for (TodoWithCategory todoWithCategory : currentList) {
                if (todoWithCategory.getTodoItem().getId() == todoItem.getId()) {
                    TodoItem updatedTodoItem = new TodoItem();
                    copyTodoItemProperties(todoWithCategory.getTodoItem(), updatedTodoItem);
                    updatedTodoItem.setCompleted(newCompletionState);
                    updatedList.add(new TodoWithCategory(
                            updatedTodoItem,
                            todoWithCategory.getCategoryName(),
                            todoWithCategory.getCategoryColor(),
                            todoWithCategory.getDisplayTitle()
                    ));
                } else {
                    updatedList.add(todoWithCategory);
                }
            }
            mFilteredTodos.setValue(updatedList);
        }

        if (todoItem.isFromCollaboration()) {
            mRepository.toggleCollaborationTodoCompletion(todoItem);
        } else {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                TodoItem itemToUpdate = todoDao.getTodoByIdSync(todoItem.getId());
                if (itemToUpdate != null) {
                    itemToUpdate.setCompleted(newCompletionState);
                    mRepository.update(itemToUpdate);
                    Log.d(TAG, "Local todo completion toggled successfully in DB");
                }
            });
        }
    }

    private void copyTodoItemProperties(TodoItem source, TodoItem target) {
        target.setId(source.getId());
        target.setTitle(source.getTitle());
        target.setContent(source.getContent());
        target.setCompleted(source.isCompleted());
        target.setCategoryId(source.getCategoryId());
        target.setLocationName(source.getLocationName());
        target.setLocationLatitude(source.getLocationLatitude());
        target.setLocationLongitude(source.getLocationLongitude());
        target.setLocationRadius(source.getLocationRadius());
        target.setLocationEnabled(source.isLocationEnabled());
        target.setLocationId(source.getLocationId());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setDueDate(source.getDueDate());
        target.setFromCollaboration(source.isFromCollaboration());
        target.setProjectId(source.getProjectId());
        target.setFirebaseTaskId(source.getFirebaseTaskId());
        target.setProjectName(source.getProjectName());
        target.setAssignedTo(source.getAssignedTo());
        target.setCreatedBy(source.getCreatedBy());
        target.setArchived(source.isArchived());
    }

    public void delete(TodoItem todoItem) {
        Log.d(TAG, "Deleting todo: " + todoItem.getTitle() + " (collaboration: " + todoItem.isFromCollaboration() + ")");
        mRepository.delete(todoItem);
    }

    public void deleteAllTodos() {
        Log.d(TAG, "Deleting all todos");
        mRepository.deleteAllTodos();
    }

    public void showAllTodos() {
        Log.d(TAG, "Showing all todos (category filter)");
        mCurrentCategoryFilter = -1;
        applyCurrentFilter(mVisibleTodosWithCategory.getValue());
    }

    public void showTodosWithoutCategory() {
        Log.d(TAG, "Showing todos without category");
        mCurrentCategoryFilter = 0;
        applyCurrentFilter(mVisibleTodosWithCategory.getValue());
    }

    public void showTodosByCategory(int categoryId) {
        Log.d(TAG, "Showing todos by category: " + categoryId);
        mCurrentCategoryFilter = categoryId;
        applyCurrentFilter(mVisibleTodosWithCategory.getValue());
    }

    public void showOnlyCollaborationTodos() {
        Log.d(TAG, "Showing only collaboration todos");
        mShowCollaborationTodos = true;
        mShowLocalTodos = false;
        applyCurrentFilter(mVisibleTodosWithCategory.getValue());
    }

    public void showOnlyLocalTodos() {
        Log.d(TAG, "Showing only local todos");
        mShowCollaborationTodos = false;
        mShowLocalTodos = true;
        applyCurrentFilter(mVisibleTodosWithCategory.getValue());
    }

    public void showAllTypes() {
        Log.d(TAG, "Showing all types of todos");
        mShowCollaborationTodos = true;
        mShowLocalTodos = true;
        applyCurrentFilter(mVisibleTodosWithCategory.getValue());
    }

    public void performManualSync() {
        Log.d(TAG, "Performing manual sync");
        try {
            mRepository.performManualSync();
            isSyncActive.setValue(true);
            syncStatusMessage.setValue("수동 동기화 실행됨");
        } catch (Exception e) {
            Log.e(TAG, "Manual sync failed", e);
            syncStatusMessage.setValue("동기화 실패: " + e.getMessage());
        }
    }

    public void startCollaborationSync() {
        Log.d(TAG, "Starting collaboration sync");
        try {
            mRepository.startCollaborationSync();
            isSyncActive.setValue(true);
            syncStatusMessage.setValue("동기화 시작됨");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start sync", e);
            isSyncActive.setValue(false);
            syncStatusMessage.setValue("동기화 시작 실패");
        }
    }

    public void stopCollaborationSync() {
        Log.d(TAG, "Stopping collaboration sync");
        try {
            mRepository.stopCollaborationSync();
            isSyncActive.setValue(false);
            syncStatusMessage.setValue("동기화 중지됨");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop sync", e);
            syncStatusMessage.setValue("동기화 중지 실패");
        }
    }

    public int getCurrentCategoryFilter() {
        return mCurrentCategoryFilter;
    }

    public boolean isShowingCollaborationTodos() {
        return mShowCollaborationTodos;
    }

    public boolean isShowingLocalTodos() {
        return mShowLocalTodos;
    }

    public LiveData<Boolean> getIsSyncActive() {
        return isSyncActive;
    }

    public LiveData<String> getSyncStatusMessage() {
        return syncStatusMessage;
    }

    public void getCollaborationTodoCount(OnCountReceivedListener listener) {
        mRepository.getCollaborationTodoCount(listener::onCountReceived);
    }

    public boolean isCollaborationSyncActive() {
        return mRepository.isCollaborationSyncActive();
    }

    public int getSyncingProjectCount() {
        return mRepository.getSyncingProjectCount();
    }

    public LiveData<Map<LocalDate, Float>> getMonthlyCompletionRates() {
        return monthlyCompletionRates;
    }

    public void setCurrentDisplayMonth(YearMonth yearMonth) {
        this.currentDisplayMonth = yearMonth;
        updateMonthlyCompletionRates(yearMonth, mAllTodosForStats.getValue());
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
                                // 기한 없는 할일은 오늘 날짜에만 포함
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
        Log.d(TAG, "ViewModel cleared, stopping collaboration sync");
        try {
            mRepository.stopCollaborationSync();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping sync in onCleared", e);
        }
    }

    public static class TodoWithCategory {
        private final TodoItem todoItem;
        private final String categoryName;
        private final String categoryColor;
        private final String displayTitle;

        public TodoWithCategory(TodoItem todoItem, String categoryName, String categoryColor, String displayTitle) {
            this.todoItem = todoItem;
            this.categoryName = categoryName;
            this.categoryColor = categoryColor;
            this.displayTitle = displayTitle;
        }

        public TodoItem getTodoItem() { return todoItem; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryColor() { return categoryColor; }
        public String getDisplayTitle() { return displayTitle; }

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

    public interface OnCountReceivedListener {
        void onCountReceived(int count);
    }
}