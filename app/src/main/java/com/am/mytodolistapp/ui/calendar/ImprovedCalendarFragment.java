package com.am.mytodolistapp.ui.calendar;

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
import com.am.mytodolistapp.ui.task.AddTodoWithDateDialogFragment;
import com.am.mytodolistapp.ui.task.TaskListViewModel;
import com.am.mytodolistapp.ui.task.TaskWithDateAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


// 캘린더와 선택된 날짜의 할 일 목록을 함께 보여주는 캘린더 프래그먼트
// 날짜별 할 일 완료율을 시각적으로 표시하고, 날짜를 선택하면 해당 날짜의 할 일을 보여준다.
public class ImprovedCalendarFragment extends Fragment {

    // UI 컴포넌트
    private TextView textCurrentMonth;
    private ImageButton buttonPreviousMonth, buttonNextMonth;
    private RecyclerView recyclerViewCalendar;
    private RecyclerView recyclerViewSelectedDateTasks;
    private FloatingActionButton fabAddTask;


    // 어댑터 및 ViewModel
    private CalendarAdapter calendarAdapter;
    private TaskWithDateAdapter selectedDateTasksAdapter;
    private TaskListViewModel taskListViewModel;


    // 상태 변수
    private LocalDate currentDate; // 현재 달력에 표시된 기준 월의 LocalDate
    private LocalDate selectedDate; // 사용자가 선택한 날짜

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ViewModel을 Activity 범위로 가져와 다른 프래그먼트와 데이터 공유
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);
        currentDate = LocalDate.now();
        selectedDate = LocalDate.now(); // 초기 선택 날짜는 오늘
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

        // 초기 캘린더 및 할 일 목록 업데이트
        updateCalendarDisplay(); // currentDate 기준으로 월 표시 및 날짜 생성
        updateSelectedDateTasks(); // selectedDate 기준으로 할 일 표시
        observeCompletionRates(); // 완료율 관찰 시작
    }

    private void initViews(View view) {
        textCurrentMonth = view.findViewById(R.id.text_current_month);
        buttonPreviousMonth = view.findViewById(R.id.button_previous_month);
        buttonNextMonth = view.findViewById(R.id.button_next_month);
        recyclerViewCalendar = view.findViewById(R.id.recycler_view_calendar);
        recyclerViewSelectedDateTasks = view.findViewById(R.id.recycler_view_selected_date_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);
    } //XML 레이아웃의 뷰들 초기화

    private void setupCalendar() {
        recyclerViewCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarAdapter(new ArrayList<>(), date -> {
            // 날짜 클릭 시, 선택된 날짜를 업데이트하고 해당 날짜의 할 일 목록을 다시 로드
            selectedDate = date;
            calendarAdapter.setSelectedDate(selectedDate);
            updateSelectedDateTasks();
        });
        recyclerViewCalendar.setAdapter(calendarAdapter);
    } // 캘린더 RecyclerView를 설정


    private void setupSelectedDateTasks() {
        recyclerViewSelectedDateTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedDateTasksAdapter = new TaskWithDateAdapter(taskListViewModel); // ViewModel 전달
        recyclerViewSelectedDateTasks.setAdapter(selectedDateTasksAdapter);
    }// 선택된 날짜의 할 일 목록 RecyclerView를 설정


    // 버튼 클릭 리스너들을 설정
    private void setupClickListeners() {
        // 이전 달 버튼
        buttonPreviousMonth.setOnClickListener(v -> {
            currentDate = currentDate.minusMonths(1);
            updateCalendarDisplay();
        });
        // 다음 달 버튼
        buttonNextMonth.setOnClickListener(v -> {
            currentDate = currentDate.plusMonths(1);
            updateCalendarDisplay();
        });
        // 할 일 추가 FAB
        fabAddTask.setOnClickListener(v -> {
            // 선택된 날짜를 기본 기한으로 하는 할 일 추가 다이얼로그 표시
            AddTodoWithDateDialogFragment dialog = AddTodoWithDateDialogFragment.newInstance(selectedDate);
            dialog.show(requireActivity().getSupportFragmentManager(), "AddTodoWithDateDialog");
        });
    }

    private void updateCalendarDisplay() {
        // 월 텍스트 업데이트
        String monthText = currentDate.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN));
        textCurrentMonth.setText(monthText);

        // 캘린더 날짜 생성 및 어댑터 업데이트
        List<CalendarDay> calendarDays = generateCalendarDays(currentDate);
        calendarAdapter.updateCalendar(calendarDays);
        calendarAdapter.setSelectedDate(selectedDate); // 현재 선택된 날짜 유지
        calendarAdapter.setToday(LocalDate.now()); // 오늘 날짜 업데이트

        //현재 표시 월을 ViewModel에 알려서 자동 완료율 업데이트 활성화
        taskListViewModel.setCurrentDisplayMonth(YearMonth.from(currentDate));
    }

    private void updateSelectedDateTasks() {

        taskListViewModel.getAllTodosWithCategoryForCalendar().observe(getViewLifecycleOwner(), allTodos -> {
            if (allTodos == null) return;
            List<TaskListViewModel.TodoWithCategory> filtered = filterTodosByDate(allTodos, selectedDate);
            selectedDateTasksAdapter.submitList(filtered);
        });
    }//선택된 날짜에 해당하는 할 일 목록을 ViewModel로부터 가져와 업데이트한다.

    private void observeCompletionRates() {
        taskListViewModel.getMonthlyCompletionRates().observe(getViewLifecycleOwner(), rates -> {
            if (rates != null) {
                calendarAdapter.setCompletionRates(rates);
            }
        });
    } // ViewModel로부터 월별 완료율 데이터를 관찰하여 캘린더 UI에 반영



    // 전체 할 일 목록에서 특정 날짜에 해당하는 할 일만 필터링
    // 기한이 있는 할 일은 기한을 기준으로, 기한이 없는 할 일은 생성일을 기준으로 필터링한다.
    private List<TaskListViewModel.TodoWithCategory> filterTodosByDate(
            List<TaskListViewModel.TodoWithCategory> allTodos, LocalDate targetDate) {
        List<TaskListViewModel.TodoWithCategory> filteredTodos = new ArrayList<>();
        if (allTodos == null || targetDate == null) return filteredTodos;

        // 대상 날짜의 시작(00:00:00)과 끝(23:59:59) 시간을 계산
        Calendar targetCalendarStart = Calendar.getInstance();
        targetCalendarStart.set(targetDate.getYear(), targetDate.getMonthValue() - 1, targetDate.getDayOfMonth(), 0, 0, 0);
        targetCalendarStart.set(Calendar.MILLISECOND, 0);
        long targetMillisStart = targetCalendarStart.getTimeInMillis();

        Calendar targetCalendarEnd = Calendar.getInstance();
        targetCalendarEnd.set(targetDate.getYear(), targetDate.getMonthValue() - 1, targetDate.getDayOfMonth(), 23, 59, 59);
        targetCalendarEnd.set(Calendar.MILLISECOND, 999);
        long targetMillisEnd = targetCalendarEnd.getTimeInMillis();

        for (TaskListViewModel.TodoWithCategory todoWithCategory : allTodos) {
            TodoItem todo = todoWithCategory.getTodoItem();
            Long dueDate = todo.getDueDate();

            if (dueDate == null) {
                // 기한 없는 할일은 생성된 날짜를 기준으로 표시
                long createdAt = todo.getCreatedAt();
                if (createdAt >= targetMillisStart && createdAt <= targetMillisEnd) {
                    filteredTodos.add(todoWithCategory);
                }
            } else {
                // 기한 있는 할일은 기한 날짜를 기준으로 표시
                if (dueDate >= targetMillisStart && dueDate <= targetMillisEnd) {
                    filteredTodos.add(todoWithCategory);
                }
            }
        }
        return filteredTodos;
    }


    //주어진 월에 대한 캘린더 날짜 목록(5주)을 생성
    private List<CalendarDay> generateCalendarDays(LocalDate date) {
        List<CalendarDay> days = new ArrayList<>();
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue(); // 월요일=1, 일요일=7
        int startOffset = (firstDayOfWeek == 7) ? 0 : firstDayOfWeek; // 일요일이 주의 시작이 되도록 조정

        LocalDate displayStartDate = firstDayOfMonth.minusDays(startOffset);

        // 월간 보기로 35일(5주)
        for (int i = 0; i < 35; i++) {
            LocalDate currentDay = displayStartDate.plusDays(i);
            boolean isCurrentDisplayMonth = currentDay.getMonth() == currentDate.getMonth();

            CalendarDay calendarDay = new CalendarDay(currentDay, isCurrentDisplayMonth);
            days.add(calendarDay);
        }
        return days;
    }
}