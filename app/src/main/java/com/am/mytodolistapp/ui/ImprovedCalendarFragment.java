package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.animation.ValueAnimator;


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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImprovedCalendarFragment extends Fragment {

    private TextView textCurrentMonth;
    private ImageButton buttonPreviousMonth, buttonNextMonth, buttonToggleCalendarView; // 버튼 추가
    private RecyclerView recyclerViewCalendar;
    private RecyclerView recyclerViewSelectedDateTasks;
    private FloatingActionButton fabAddTask;

    private CalendarAdapter calendarAdapter;
    private TaskWithDateAdapter selectedDateTasksAdapter;
    private TaskListViewModel taskListViewModel;

    private LocalDate currentDate; // 현재 달력에 표시된 기준 월의 LocalDate
    private LocalDate selectedDate; // 사용자가 선택한 날짜

    // 캘린더 보기 상태 관련
    private boolean isMonthView = true; // true: 월간, false: 주간
    private final int WEEK_CALENDAR_TARGET_HEIGHT_DP = 100; // 주간 캘린더 높이 (예시)
    private final int MONTH_CALENDAR_TARGET_HEIGHT_DP = 250; // 월간 캘린더 높이 (예시)

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        updateCalendarViewIcon(); // 초기 아이콘 설정
    }

    private void initViews(View view) {
        textCurrentMonth = view.findViewById(R.id.text_current_month);
        buttonPreviousMonth = view.findViewById(R.id.button_previous_month);
        buttonNextMonth = view.findViewById(R.id.button_next_month);
        buttonToggleCalendarView = view.findViewById(R.id.button_toggle_calendar_view); // ID 확인 필요
        recyclerViewCalendar = view.findViewById(R.id.recycler_view_calendar);
        recyclerViewSelectedDateTasks = view.findViewById(R.id.recycler_view_selected_date_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);
    }

    private void setupCalendar() {
        recyclerViewCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarAdapter(new ArrayList<>(), date -> {
            selectedDate = date; // 사용자가 날짜를 클릭하면 selectedDate 업데이트
            calendarAdapter.setSelectedDate(selectedDate);
            updateSelectedDateTasks(); // 선택된 날짜에 맞는 할 일 목록 로드
        });
        recyclerViewCalendar.setAdapter(calendarAdapter);
    }

    private void setupSelectedDateTasks() {
        recyclerViewSelectedDateTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedDateTasksAdapter = new TaskWithDateAdapter(taskListViewModel); // ViewModel 전달
        recyclerViewSelectedDateTasks.setAdapter(selectedDateTasksAdapter);
    }

    private void setupClickListeners() {
        buttonPreviousMonth.setOnClickListener(v -> {
            currentDate = currentDate.minusMonths(1);
            updateCalendarDisplay();
        });

        buttonNextMonth.setOnClickListener(v -> {
            currentDate = currentDate.plusMonths(1);
            updateCalendarDisplay();
        });

        fabAddTask.setOnClickListener(v -> {
            AddTodoWithDateDialogFragment dialog = AddTodoWithDateDialogFragment.newInstance(selectedDate);
            dialog.show(requireActivity().getSupportFragmentManager(), "AddTodoWithDateDialog");
        });

        buttonToggleCalendarView.setOnClickListener(v -> toggleCalendarView());
    }

    private void updateCalendarDisplay() {
        // 월 텍스트 업데이트
        String monthText = currentDate.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN));
        textCurrentMonth.setText(monthText);

        // 캘린더 날짜 생성 및 어댑터 업데이트
        List<CalendarFragment.CalendarDay> calendarDays = generateCalendarDays(currentDate);
        calendarAdapter.updateCalendar(calendarDays);
        calendarAdapter.setSelectedDate(selectedDate); // 현재 선택된 날짜 유지
        calendarAdapter.setToday(LocalDate.now()); // 오늘 날짜 업데이트

        // 현재 표시된 월의 완료율 요청
        taskListViewModel.updateMonthlyCompletionRates(YearMonth.from(currentDate), taskListViewModel.getAllTodosWithCategory().getValue());
    }


    private void updateSelectedDateTasks() {
        taskListViewModel.getAllTodosWithCategory().observe(getViewLifecycleOwner(), allTodos -> {
            if (allTodos == null) return;
            List<TaskListViewModel.TodoWithCategory> filtered = filterTodosByDate(allTodos, selectedDate);
            selectedDateTasksAdapter.submitList(filtered);
        });
    }

    private void observeCompletionRates() {
        taskListViewModel.getMonthlyCompletionRates().observe(getViewLifecycleOwner(), rates -> {
            if (rates != null) {
                calendarAdapter.setCompletionRates(rates);
            }
        });
    }

    private List<TaskListViewModel.TodoWithCategory> filterTodosByDate(
            List<TaskListViewModel.TodoWithCategory> allTodos, LocalDate targetDate) {
        List<TaskListViewModel.TodoWithCategory> filteredTodos = new ArrayList<>();
        if (allTodos == null || targetDate == null) return filteredTodos;

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

            if (dueDate == null) { // 기한 없는 할일
                if (targetDate.equals(LocalDate.now())) { // 오늘 날짜에만 표시
                    filteredTodos.add(todoWithCategory);
                }
            } else { // 기한 있는 할일
                if (dueDate >= targetMillisStart && dueDate <= targetMillisEnd) {
                    filteredTodos.add(todoWithCategory);
                }
            }
        }
        return filteredTodos;
    }

    private List<CalendarFragment.CalendarDay> generateCalendarDays(LocalDate date) {
        List<CalendarFragment.CalendarDay> days = new ArrayList<>();
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue(); // MONDAY=1, SUNDAY=7
        int startOffset = (firstDayOfWeek == 7) ? 0 : firstDayOfWeek; // 일요일이 주의 시작이 되도록 조정

        LocalDate startDate = firstDayOfMonth.minusDays(startOffset);

        int rows = isMonthView ? 6 : 1; // 월간이면 6주, 주간이면 1주 (실제 날짜 생성은 월 단위로 하고, 보여주는 부분만 조정 가능)
        // generateCalendarDays는 항상 현재 "currentDate"의 월 전체를 기준으로 생성하고,
        // 실제 RecyclerView에 표시되는 아이템 수는 Adapter나 LayoutManager에서 제한하거나,
        // 혹은 표시할 주의 날짜만 필터링해서 Adapter에 전달하는 방식이 필요합니다.
        // 여기서는 월 전체 날짜를 생성하고, RecyclerView 높이로 주간/월간을 표현합니다.
        // 주간보기 시, selectedDate가 포함된 주를 보여줘야 합니다.

        LocalDate displayStartDate = startDate;
        if (!isMonthView) { // 주간 보기일 경우, 선택된 날짜가 포함된 주의 시작일(일요일)로 설정
            displayStartDate = selectedDate;
            while(displayStartDate.getDayOfWeek().getValue() != 7) { // 일요일까지 이전으로 이동
                displayStartDate = displayStartDate.minusDays(1);
            }
        }


        // 6주(42일) 또는 주간보기 시 해당 주만 생성하도록 수정 필요.
        // 간소화를 위해 월간보기처럼 6주치 날짜를 생성하고, 캘린더 높이로 주간/월간을 표현합니다.
        // 주간보기 시에는 selectedDate가 포함된 주를 계산해서 보여줘야 합니다.
        // 아래는 월간보기 기준 날짜 생성 로직 유지, 주간보기는 높이조절로만.
        for (int i = 0; i < 42; i++) { // 항상 6주치 생성
            LocalDate currentDay = displayStartDate.plusDays(i);
            // 주간 보기일때는 현재 보고있는 월(currentDate)의 날짜가 아니어도 됨
            boolean isCurrentDisplayMonth = currentDay.getMonth() == currentDate.getMonth();
            if (!isMonthView) { // 주간 보기일 때는 항상 현재 월로 취급 (흐리게 표시 안함)
                isCurrentDisplayMonth = true;
            }

            days.add(new CalendarFragment.CalendarDay(currentDay, isCurrentDisplayMonth));
            if (!isMonthView && i == 6) break; // 주간 보기일 경우 7일만 추가
        }
        return days;
    }


    private void toggleCalendarView() {
        isMonthView = !isMonthView;
        updateCalendarViewIcon();

        final int targetHeightPx = (int) (getResources().getDisplayMetrics().density *
                (isMonthView ? MONTH_CALENDAR_TARGET_HEIGHT_DP : WEEK_CALENDAR_TARGET_HEIGHT_DP));

        ValueAnimator heightAnimator = ValueAnimator.ofInt(recyclerViewCalendar.getHeight(), targetHeightPx);
        heightAnimator.setDuration(300); // 애니메이션 시간 (ms)
        heightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        heightAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = recyclerViewCalendar.getLayoutParams();
            params.height = (Integer) animation.getAnimatedValue();
            recyclerViewCalendar.setLayoutParams(params);
        });
        heightAnimator.start();

        // 주간 보기일 경우, 선택된 날짜가 포함된 주로 캘린더를 다시 그려야 할 수 있음
        if (!isMonthView) {
            currentDate = selectedDate; // 주간 보기의 기준을 선택된 날짜의 월로 변경하고 다시 그림
        }
        updateCalendarDisplay(); // 캘린더 내용(날짜 생성)도 갱신
    }

    private void updateCalendarViewIcon() {
        if (isMonthView) {
            buttonToggleCalendarView.setImageResource(R.drawable.ic_expand_less); // 접기 아이콘
        } else {
            buttonToggleCalendarView.setImageResource(R.drawable.ic_expand_more); // 펴기 아이콘
        }
    }
}