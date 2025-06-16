package com.am.mytodolistapp.ui.collaboration;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

//특정 프로젝트의 할 일 목록을 보여주는 프래그먼트
// 사용자는 이 화면에서 할 일을 조회, 추가, 수정, 삭제할 수 있으며, 멤버 목록을 확인할 수 있다
public class ProjectTaskListFragment extends Fragment {

    private static final String ARG_PROJECT_ID = "project_id";
    private static final String ARG_PROJECT_NAME = "project_name";

    private ProjectTaskListViewModel viewModel;
    private RecyclerView recyclerViewTasks;
    private ProjectTaskAdapter taskAdapter;
    private FloatingActionButton fabAddTask;

    private String projectId;
    private String projectName;
    private com.am.mytodolistapp.data.firebase.Project project; // 현재 프로젝트 정보

    public static ProjectTaskListFragment newInstance(String projectId, String projectName) {
        ProjectTaskListFragment fragment = new ProjectTaskListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        args.putString(ARG_PROJECT_NAME, projectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);


        if (getArguments() != null) {
            projectId = getArguments().getString(ARG_PROJECT_ID);
            projectName = getArguments().getString(ARG_PROJECT_NAME);
        }

        viewModel = new ViewModelProvider(this).get(ProjectTaskListViewModel.class);
        viewModel.setProjectId(projectId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_project_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 툴바 제목을 현재 프로젝트 이름으로 설정
        if (getActivity() instanceof AppCompatActivity && projectName != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(projectName);
        }

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        observeData();
    }

    private void initViews(View view) {
        recyclerViewTasks = view.findViewById(R.id.recycler_view_project_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_project_task);
    }

    private void setupRecyclerView() {
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new ProjectTaskAdapter(
                task -> viewModel.toggleTaskCompletion(task),
                this::showEditTaskDialog,
                task -> viewModel.deleteTask(task.getTaskId())
        );
        recyclerViewTasks.setAdapter(taskAdapter);
    }

    private void setupClickListeners() {
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }


    //ViewModel의 LiveData를 관찰하여 UI를 업데이트합니다.
    private void observeData() {
        // 프로젝트 할 일 목록 관찰
        viewModel.getProjectTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                taskAdapter.submitList(tasks);
            }
        });
        // 오류 메시지 관찰
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
        // 현재 프로젝트 정보 관찰
        viewModel.getCurrentProject().observe(getViewLifecycleOwner(), currentProject -> {
            this.project = currentProject;

            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        });
        // 프로젝트 멤버 목록 관찰
        viewModel.getProjectMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null && !members.isEmpty() && project != null) {
                showProjectMembersDialog(members, project);
            }
        });
        viewModel.getIsLoadingMembers().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                Log.d("ProjectTaskList", "Loading members...");
            }
        });
    }

    //옵션 메뉴를 생성
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.project_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    //옵션 메뉴 아이템 선택 이벤트를 처리
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_invite_member) {
            // '멤버 목록' 메뉴 클릭 시, ViewModel을 통해 멤버 목록 로드를 시작
            viewModel.loadProjectMembers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 새 할 일 추가 다이얼로그를 표시
    private void showAddTaskDialog() {
        AddProjectTaskDialogFragment dialog = new AddProjectTaskDialogFragment();
        dialog.setOnTaskAddedListener((title, content, dueDate) -> {
            viewModel.addTask(title, content, dueDate);
        });
        dialog.show(getChildFragmentManager(), "AddProjectTaskDialog");
    }


    //할 일 수정 다이얼로그를 표시
    private void showEditTaskDialog(com.am.mytodolistapp.data.firebase.ProjectTask task) {
        EditProjectTaskDialogFragment dialog = EditProjectTaskDialogFragment.newInstance(task);
        dialog.setOnTaskUpdatedListener(viewModel::updateTask);
        dialog.show(getChildFragmentManager(), "EditProjectTaskDialog");
    }

    //프로젝트 멤버 목록 다이얼로그를 표시
    private void showProjectMembersDialog(java.util.List<com.am.mytodolistapp.data.firebase.User> members,
                                          com.am.mytodolistapp.data.firebase.Project project) {
        ProjectMembersDialogFragment dialog = ProjectMembersDialogFragment.newInstance(members, projectName, project);
        dialog.show(getChildFragmentManager(), "ProjectMembersDialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {

        }
    }
}