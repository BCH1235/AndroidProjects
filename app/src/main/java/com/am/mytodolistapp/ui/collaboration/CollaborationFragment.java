package com.am.mytodolistapp.ui.collaboration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CollaborationFragment extends Fragment {

    private CollaborationViewModel viewModel;
    private RecyclerView recyclerViewProjects;
    private RecyclerView recyclerViewInvitations;
    private ProjectListAdapter projectAdapter;
    private InvitationListAdapter invitationAdapter;
    private FloatingActionButton fabAddProject;

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

        // 사용자 인증 확인
        if (!FirebaseRepository.getInstance().isUserLoggedIn()) {
            // 로그인 화면으로 이동하거나 로그인 프래그먼트 표시
            showLoginRequired();
            return;
        }

        initViews(view);
        setupRecyclerViews();
        setupClickListeners();
        observeData();
    }

    private void showLoginRequired() {
        Toast.makeText(getContext(), "협업 기능을 사용하려면 로그인이 필요합니다.", Toast.LENGTH_LONG).show();
        // TODO: 로그인 Fragment로 이동하는 로직 구현
    }

    private void initViews(View view) {
        recyclerViewProjects = view.findViewById(R.id.recycler_view_projects);
        recyclerViewInvitations = view.findViewById(R.id.recycler_view_invitations);
        fabAddProject = view.findViewById(R.id.fab_add_project);
    }

    private void setupRecyclerViews() {
        // 프로젝트 목록 RecyclerView 설정
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));
        projectAdapter = new ProjectListAdapter(project -> {
            // 프로젝트 클릭 시 해당 프로젝트의 할 일 목록으로 이동
            ProjectTaskListFragment taskListFragment = ProjectTaskListFragment.newInstance(
                    project.getProjectId(), project.getProjectName());

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, taskListFragment)
                    .addToBackStack(null)
                    .commit();
        }, project -> {
            // 멤버 초대 버튼 클릭
            showInviteMemberDialog(project);
        });
        recyclerViewProjects.setAdapter(projectAdapter);

        // 초대 목록 RecyclerView 설정
        recyclerViewInvitations.setLayoutManager(new LinearLayoutManager(getContext()));
        invitationAdapter = new InvitationListAdapter(
                invitation -> viewModel.respondToInvitation(invitation, true), // 수락
                invitation -> viewModel.respondToInvitation(invitation, false) // 거절
        );
        recyclerViewInvitations.setAdapter(invitationAdapter);
    }

    private void setupClickListeners() {
        fabAddProject.setOnClickListener(v -> showCreateProjectDialog());
    }

    private void observeData() {
        // 프로젝트 목록 관찰
        viewModel.getUserProjects().observe(getViewLifecycleOwner(), projects -> {
            if (projects != null) {
                projectAdapter.submitList(projects);
            }
        });

        // 초대 목록 관찰
        viewModel.getUserInvitations().observe(getViewLifecycleOwner(), invitations -> {
            if (invitations != null) {
                invitationAdapter.submitList(invitations);
                // 초대가 있으면 RecyclerView 표시, 없으면 숨김
                recyclerViewInvitations.setVisibility(
                        invitations.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });

        // 에러 메시지 관찰
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // 성공 메시지 관찰
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), success -> {
            if (success != null && !success.isEmpty()) {
                Toast.makeText(getContext(), success, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateProjectDialog() {
        CreateProjectDialogFragment dialog = new CreateProjectDialogFragment();
        dialog.setOnProjectCreatedListener((projectName, description) -> {
            viewModel.createProject(projectName, description);
        });
        dialog.show(getChildFragmentManager(), "CreateProjectDialog");
    }

    private void showInviteMemberDialog(com.am.mytodolistapp.data.firebase.Project project) {
        InviteMemberDialogFragment dialog = InviteMemberDialogFragment.newInstance(
                project.getProjectId(), project.getProjectName());
        dialog.setOnMemberInvitedListener(inviteeEmail -> {
            viewModel.sendInvitation(project.getProjectId(), project.getProjectName(), inviteeEmail);
        });
        dialog.show(getChildFragmentManager(), "InviteMemberDialog");
    }
}