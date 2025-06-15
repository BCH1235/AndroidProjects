package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class StatisticsFragment extends Fragment {

    private TextView textCompletedTasks;
    private TextView textPendingTasks;
    private TextView textOverviewTitle;
    private DailyCompletionBarChart dailyChart;
    private CategoryPieChart categoryChart;
    private RecyclerView categoryLegendRecycler;
    private CardView legendCard;
    private CategoryLegendAdapter legendAdapter;

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
        setupRecyclerView();
        observeData();
        setupUserNickname();
    }

    private void initViews(View view) {
        textOverviewTitle = view.findViewById(R.id.text_overview_title);
        textCompletedTasks = view.findViewById(R.id.text_completed_tasks);
        textPendingTasks = view.findViewById(R.id.text_pending_tasks);
        dailyChart = view.findViewById(R.id.daily_completion_chart);
        categoryChart = view.findViewById(R.id.category_pie_chart);
        categoryLegendRecycler = view.findViewById(R.id.category_legend_recycler);
        legendCard = view.findViewById(R.id.legend_card);
    }

    // 닉네임 설정 메서드 추가
    private void setupUserNickname() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            String title = currentUser.getDisplayName() + "님의 작업 개요";
            textOverviewTitle.setText(title);
        } else {
            // 닉네임이 없는 경우 기본 텍스트 유지
            textOverviewTitle.setText("작업 개요");
        }
    }
    private void setupRecyclerView() {
        legendAdapter = new CategoryLegendAdapter(new ArrayList<>());
        categoryLegendRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryLegendRecycler.setAdapter(legendAdapter);
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

            // 범례 업데이트
            if (categoryData != null && !categoryData.isEmpty()) {
                legendAdapter.updateData(categoryData);
                legendCard.setVisibility(View.VISIBLE);
            } else {
                legendCard.setVisibility(View.GONE);
            }
        });
    }
}