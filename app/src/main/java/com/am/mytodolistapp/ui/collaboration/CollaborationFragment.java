package com.am.mytodolistapp.ui.collaboration;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;


//협업 기능의 메인 화면을 담당하는 프래그먼트
// 사용자가 참여 중인 프로젝트 목록과 받은 초대 목록을 보여준다
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

        // 사용자가 로그인했는지 확인. 로그인하지 않았다면 기능 사용 불가능하다
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
        }, project -> {
            // 프로젝트 길게 누르면 삭제 확인 대화상자 표시
            showDeleteProjectDialog(project);
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


    //플로팅 버튼
    private void setupClickListeners() {
        fabAddProject.setOnClickListener(v -> showCreateProjectDialog());
    }

    private void observeData() {
        // 프로젝트 목록 LiveData 관찰
        viewModel.getUserProjects().observe(getViewLifecycleOwner(), projects -> {
            Log.d("CollaborationFragment", "Projects received: " + (projects != null ? projects.size() : "null"));
            if (projects != null) {
                // 기존 방식
                projectAdapter.submitList(projects);

                // 강제 새로고침을 위한 추가 코드
                projectAdapter.submitList(null);
                projectAdapter.submitList(new ArrayList<>(projects));
            }
        });

        // 초대 목록 LiveData 관찰
        viewModel.getUserInvitations().observe(getViewLifecycleOwner(), invitations -> {
            Log.d("CollaborationFragment", "Invitations received: " + (invitations != null ? invitations.size() : "null"));
            if (invitations != null) {
                invitationAdapter.submitList(null);
                invitationAdapter.submitList(new ArrayList<>(invitations));

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

    //새 프로젝트 생성 다이얼로그를 표시
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
    } // 멤버 초대 다이얼로그를 표시

    // 프로젝트 삭제 확인 대화상자 표시
    private void showDeleteProjectDialog(com.am.mytodolistapp.data.firebase.Project project) {
        new AlertDialog.Builder(getContext())
                .setTitle("프로젝트 삭제")
                .setMessage("'" + project.getProjectName() + "' 프로젝트를 정말 삭제하시겠습니까?\n\n" +
                        "프로젝트와 관련된 모든 할 일도 함께 삭제됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    // 삭제 확인 시 ViewModel의 deleteProject 호출
                    viewModel.deleteProject(project);
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    // 취소 시 대화상자만 닫기
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
}