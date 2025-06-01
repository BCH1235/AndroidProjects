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
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 캘린더 뷰를 표시하고 관리하는 프래그먼트
public class CalendarFragment extends Fragment {

    private TextView textCurrentMonth;
    private ImageButton buttonPreviousMonth, buttonNextMonth;
    private RecyclerView recyclerViewCalendar;
    private FloatingActionButton fabAddTask;

    private CalendarAdapter calendarAdapter;
    private TaskListViewModel taskListViewModel; // 기존 ViewModel 사용

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
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupCalendar();
        setupClickListeners();
        updateCalendar();

        //완료율 관찰
        observeCompletionRates();
    }

    private void initViews(View view) {
        textCurrentMonth = view.findViewById(R.id.text_current_month);
        buttonPreviousMonth = view.findViewById(R.id.button_previous_month);
        buttonNextMonth = view.findViewById(R.id.button_next_month);
        recyclerViewCalendar = view.findViewById(R.id.recycler_view_calendar);
        fabAddTask = view.findViewById(R.id.fab_add_task);
    }

    private void setupCalendar() {
        // 7일 단위로 표시 (일주일)
        recyclerViewCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));

        calendarAdapter = new CalendarAdapter(new ArrayList<>(), date -> {
            selectedDate = date;
            calendarAdapter.setSelectedDate(selectedDate);
        });

        recyclerViewCalendar.setAdapter(calendarAdapter);
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
            // 기존 할 일 추가 다이얼로그 사용 (기존 스타일 유지)
            AddTodoDialogFragment dialog = new AddTodoDialogFragment();
            dialog.show(requireActivity().getSupportFragmentManager(), "AddTodoDialog");
        });
    }

    private void updateCalendar() {
        // 현재 월 텍스트 업데이트 (간단한 형식)
        String monthText = currentDate.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN));
        textCurrentMonth.setText(monthText);

        // 캘린더 날짜 생성
        List<CalendarDay> calendarDays = generateCalendarDays(currentDate);
        calendarAdapter.updateCalendar(calendarDays);
        calendarAdapter.setSelectedDate(selectedDate);
        calendarAdapter.setToday(LocalDate.now());

        //완료율 기능을 위한 현재 표시 월 설정
        taskListViewModel.setCurrentDisplayMonth(java.time.YearMonth.from(currentDate));
    }

    private List<CalendarDay> generateCalendarDays(LocalDate date) {
        List<CalendarDay> days = new ArrayList<>();

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

            CalendarDay calendarDay = new CalendarDay(currentDay, isCurrentMonth);
            days.add(calendarDay);
        }

        return days;
    }

    //완료율 관찰 메서드
    private void observeCompletionRates() {
        taskListViewModel.getMonthlyCompletionRates().observe(getViewLifecycleOwner(), rates -> {
            if (rates != null) {
                calendarAdapter.setCompletionRates(rates);
            }
        });
    }
}