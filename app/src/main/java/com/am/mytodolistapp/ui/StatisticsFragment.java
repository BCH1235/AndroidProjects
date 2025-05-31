package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;

public class StatisticsFragment extends Fragment {

    private TextView textCompletedTasks;
    private TextView textPendingTasks;
    private DailyCompletionBarChart dailyChart;
    private CategoryPieChart categoryChart;

    private StatisticsViewModel statisticsViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statisticsViewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        observeData();
    }

    private void initViews(View view) {
        textCompletedTasks = view.findViewById(R.id.text_completed_tasks);
        textPendingTasks = view.findViewById(R.id.text_pending_tasks);
        dailyChart = view.findViewById(R.id.daily_completion_chart);
        categoryChart = view.findViewById(R.id.category_pie_chart);
    }

    private void observeData() {
        // 완료된 작업과 보류중인 작업 수 관찰
        statisticsViewModel.getCompletedTasksCount().observe(getViewLifecycleOwner(), count -> {
            textCompletedTasks.setText(String.valueOf(count));
        });

        statisticsViewModel.getPendingTasksCount().observe(getViewLifecycleOwner(), count -> {
            textPendingTasks.setText(String.valueOf(count));
        });

        // 일일 완료 작업 데이터 관찰
        statisticsViewModel.getDailyCompletionData().observe(getViewLifecycleOwner(), dailyData -> {
            dailyChart.updateData(dailyData);
        });

        // 카테고리별 미완료 작업 데이터 관찰
        statisticsViewModel.getIncompleteByCategoryData().observe(getViewLifecycleOwner(), categoryData -> {
            categoryChart.updateData(categoryData);
        });
    }
}