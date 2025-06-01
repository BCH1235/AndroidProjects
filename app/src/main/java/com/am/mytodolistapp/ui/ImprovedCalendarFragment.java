package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ImprovedCalendarFragment extends Fragment {

    private TextView textCurrentMonth;
    private ImageButton buttonPreviousMonth, buttonNextMonth;
    private RecyclerView recyclerViewCalendar;
    private RecyclerView recyclerViewSelectedDateTasks;
    private FloatingActionButton fabAddTask;

    private CalendarAdapter calendarAdapter;
    private TaskWithDateAdapter selectedDateTasksAdapter;
    private TaskListViewModel taskListViewModel;

    private LocalDate currentDate;
    private LocalDate selectedDate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);
        currentDate = LocalDate.now();
        selectedDate = LocalDate.now();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_improved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupCalendar();
        setupSelectedDateTasks();
        setupClickListeners();
        updateCalendar();
        updateSelectedDateTasks();
    }

    private void initViews(View view) {
        textCurrentMonth = view.findViewById(R.id.text_current_month);
        buttonPreviousMonth = view.findViewById(R.id.button_previous_month);
        buttonNextMonth = view.findViewById(R.id.button_next_month);
        recyclerViewCalendar = view.findViewById(R.id.recycler_view_calendar);
        recyclerViewSelectedDateTasks = view.findViewById(R.id.recycler_view_selected_date_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);
    }

    private void setupCalendar() {
        // 7일 단위로 표시 (일주일)
        recyclerViewCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));

        calendarAdapter = new CalendarAdapter(new ArrayList<>(), date -> {
            selectedDate = date;
            calendarAdapter.setSelectedDate(selectedDate);
            updateSelectedDateTasks();
        });

        recyclerViewCalendar.setAdapter(calendarAdapter);
    }

    private void setupSelectedDateTasks() {
        recyclerViewSelectedDateTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedDateTasksAdapter = new TaskWithDateAdapter(taskListViewModel);
        recyclerViewSelectedDateTasks.setAdapter(selectedDateTasksAdapter);
    }

    private void setupClickListeners() {
        buttonPreviousMonth.setOnClickListener(v -> {
            currentDate = currentDate.minusMonths(1);
            updateCalendar();
        });

        buttonNextMonth.setOnClickListener(v -> {
            currentDate = currentDate.plusMonths(1);
            updateCalendar();
        });

        fabAddTask.setOnClickListener(v -> {
            // 선택된 날짜로 할일 추가
            AddTodoWithDateDialogFragment dialog = AddTodoWithDateDialogFragment.newInstance(selectedDate);
            dialog.show(requireActivity().getSupportFragmentManager(), "AddTodoWithDateDialog");
        });
    }

    private void updateCalendar() {
        // 현재 월 텍스트 업데이트
        String monthText = currentDate.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN));
        textCurrentMonth.setText(monthText);

        // 캘린더 날짜 생성
        List<CalendarFragment.CalendarDay> calendarDays = generateCalendarDays(currentDate);
        calendarAdapter.updateCalendar(calendarDays);
        calendarAdapter.setSelectedDate(selectedDate);
        calendarAdapter.setToday(LocalDate.now());
    }

    private void updateSelectedDateTasks() {
        // 선택된 날짜의 할일만 필터링하여 표시
        taskListViewModel.getAllTodosWithCategory().observe(getViewLifecycleOwner(), allTodos -> {
            List<TaskListViewModel.TodoWithCategory> selectedDateTodos = filterTodosByDate(allTodos, selectedDate);
            selectedDateTasksAdapter.submitList(selectedDateTodos);
        });
    }

    private List<TaskListViewModel.TodoWithCategory> filterTodosByDate(
            List<TaskListViewModel.TodoWithCategory> allTodos, LocalDate targetDate) {

        List<TaskListViewModel.TodoWithCategory> filteredTodos = new ArrayList<>();

        // LocalDate를 Calendar로 변환
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.set(targetDate.getYear(), targetDate.getMonthValue() - 1, targetDate.getDayOfMonth());
        targetCalendar.set(Calendar.HOUR_OF_DAY, 0);
        targetCalendar.set(Calendar.MINUTE, 0);
        targetCalendar.set(Calendar.SECOND, 0);
        targetCalendar.set(Calendar.MILLISECOND, 0);

        Calendar nextDayCalendar = Calendar.getInstance();
        nextDayCalendar.setTime(targetCalendar.getTime());
        nextDayCalendar.add(Calendar.DAY_OF_YEAR, 1);

        for (TaskListViewModel.TodoWithCategory todoWithCategory : allTodos) {
            TodoItem todo = todoWithCategory.getTodoItem();

            // 기한이 없는 할일은 오늘 날짜에만 표시
            if (todo.getDueDate() == null) {
                if (targetDate.equals(LocalDate.now())) {
                    filteredTodos.add(todoWithCategory);
                }
            } else {
                // 기한이 있는 할일은 해당 날짜에 표시
                long dueDateTime = todo.getDueDate();
                if (dueDateTime >= targetCalendar.getTimeInMillis() &&
                        dueDateTime < nextDayCalendar.getTimeInMillis()) {
                    filteredTodos.add(todoWithCategory);
                }
            }
        }

        return filteredTodos;
    }

    private List<CalendarFragment.CalendarDay> generateCalendarDays(LocalDate date) {
        List<CalendarFragment.CalendarDay> days = new ArrayList<>();

        // 해당 월의 첫 번째 날
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);
        // 첫 번째 날의 요일 (1=월요일, 7=일요일)
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue();
        // 일요일을 첫 번째로 만들기 위해 조정
        int startOffset = firstDayOfWeek % 7;

        // 이전 달의 마지막 날들로 빈 칸 채우기
        LocalDate startDate = firstDayOfMonth.minusDays(startOffset);

        // 6주 분량의 날짜 생성 (42일)
        for (int i = 0; i < 42; i++) {
            LocalDate currentDay = startDate.plusDays(i);
            boolean isCurrentMonth = currentDay.getMonth() == date.getMonth();

            CalendarFragment.CalendarDay calendarDay = new CalendarFragment.CalendarDay(currentDay, isCurrentMonth);
            days.add(calendarDay);
        }

        return days;
    }
}