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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CollaborationFragment extends Fragment {

    private RecyclerView recyclerViewProjects;
    private TextView textEmptyMessage;
    private FloatingActionButton fabAddProject;
    private CollaborationProjectAdapter projectAdapter;
    private CollaborationViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CollaborationViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collaboration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        observeData();
    }

    private void initViews(View view) {
        recyclerViewProjects = view.findViewById(R.id.recycler_view_projects);
        textEmptyMessage = view.findViewById(R.id.text_empty_message);
        fabAddProject = view.findViewById(R.id.fab_add_project);
    }

    private void setupRecyclerView() {
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));
        projectAdapter = new CollaborationProjectAdapter(viewModel);
        recyclerViewProjects.setAdapter(projectAdapter);
    }

    private void setupClickListeners() {
        fabAddProject.setOnClickListener(v -> {
            CreateProjectDialogFragment dialog = new CreateProjectDialogFragment();
            dialog.show(requireActivity().getSupportFragmentManager(), "CreateProjectDialog");
        });
    }

    private void observeData() {
        // 현재 사용자의 프로젝트 목록 관찰
        viewModel.getUserProjects().observe(getViewLifecycleOwner(), projects -> {
            projectAdapter.submitList(projects);

            // 빈 목록 처리
            if (projects == null || projects.isEmpty()) {
                textEmptyMessage.setVisibility(View.VISIBLE);
                recyclerViewProjects.setVisibility(View.GONE);
            } else {
                textEmptyMessage.setVisibility(View.GONE);
                recyclerViewProjects.setVisibility(View.VISIBLE);
            }
        });

        // 대기 중인 초대 알림
        viewModel.getPendingInvitations().observe(getViewLifecycleOwner(), invitations -> {
            if (invitations != null && !invitations.isEmpty()) {
                // 초대 알림 표시 (예: 스낵바, 알림 배지 등)
                showPendingInvitationsNotification(invitations.size());
            }
        });
    }

    private void showPendingInvitationsNotification(int count) {
        // 초대 알림을 위한 UI 업데이트
        // 예: 앱바에 배지 표시, 스낵바 메시지 등
    }
}