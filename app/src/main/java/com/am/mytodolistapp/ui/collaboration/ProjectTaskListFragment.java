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

        // 액션바 제목 설정
        if (getActivity() != null && projectName != null) {
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
                task -> viewModel.toggleTaskCompletion(task), // 완료 토글
                task -> showEditTaskDialog(task), // 편집
                task -> viewModel.deleteTask(task.getTaskId()) // 삭제
        );
        recyclerViewTasks.setAdapter(taskAdapter);
    }

    private void setupClickListeners() {
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void observeData() {
        // 할 일 목록 관찰
        viewModel.getProjectTasks().observe(getViewLifecycleOwner(), tasks -> {
            Log.d("ProjectTaskList", "Tasks received: " + (tasks != null ? tasks.size() : "null"));
            if (tasks != null) {
                // DiffUtil이 제대로 작동하도록 직접 submitList만 호출
                taskAdapter.submitList(tasks);
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

        // 프로젝트 정보 관찰
        viewModel.getCurrentProject().observe(getViewLifecycleOwner(), currentProject -> {
            this.project = currentProject;
        });

        // 프로젝트 멤버 목록 관찰
        viewModel.getProjectMembers().observe(getViewLifecycleOwner(), members -> {
            // 멤버 목록이 로드되면 다이얼로그 표시
            if (members != null && !members.isEmpty() && project != null) {
                showProjectMembersDialog(members, project);
            }
        });

        // 멤버 로딩 상태 관찰
        viewModel.getIsLoadingMembers().observe(getViewLifecycleOwner(), isLoading -> {
            // 로딩 상태에 따라 UI 업데이트 (필요시)
            if (isLoading != null && isLoading) {
                Log.d("ProjectTaskList", "Loading members...");
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.project_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_invite_member) {
            // 멤버 목록 보기 기능으로 변경
            viewModel.loadProjectMembers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddTaskDialog() {
        AddProjectTaskDialogFragment dialog = new AddProjectTaskDialogFragment();
        dialog.setOnTaskAddedListener((title, content, dueDate) -> {
            viewModel.addTask(title, content, dueDate);
        });
        dialog.show(getChildFragmentManager(), "AddProjectTaskDialog");
    }

    private void showEditTaskDialog(com.am.mytodolistapp.data.firebase.ProjectTask task) {
        EditProjectTaskDialogFragment dialog = EditProjectTaskDialogFragment.newInstance(task);
        dialog.setOnTaskUpdatedListener(updatedTask -> {
            viewModel.updateTask(updatedTask);
        });
        dialog.show(getChildFragmentManager(), "EditProjectTaskDialog");
    }

    private void showProjectMembersDialog(java.util.List<com.am.mytodolistapp.data.firebase.User> members,
                                          com.am.mytodolistapp.data.firebase.Project project) {
        ProjectMembersDialogFragment dialog = ProjectMembersDialogFragment.newInstance(members, projectName, project);
        dialog.show(getChildFragmentManager(), "ProjectMembersDialog");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 액션바 제목 복원
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("MyTodoList");
        }
    }
}