package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.am.mytodolistapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ProjectDetailFragment extends Fragment {

    private static final String ARG_PROJECT_ID = "project_id";

    private String projectId;
    private TextView textProjectName;
    private TextView textProjectDescription;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabAddTodo;
    private CollaborationViewModel viewModel;
    private ProjectDetailPagerAdapter pagerAdapter;

    public static ProjectDetailFragment newInstance(String projectId) {
        ProjectDetailFragment fragment = new ProjectDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            projectId = getArguments().getString(ARG_PROJECT_ID);
        }

        viewModel = new ViewModelProvider(requireActivity()).get(CollaborationViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_project_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewPager();
        setupClickListeners();
        observeData();
    }

    private void initViews(View view) {
        textProjectName = view.findViewById(R.id.text_project_name);
        textProjectDescription = view.findViewById(R.id.text_project_description);
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        fabAddTodo = view.findViewById(R.id.fab_add_todo);
    }

    private void setupViewPager() {
        pagerAdapter = new ProjectDetailPagerAdapter(this, projectId);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("할일 목록");
                    break;
                case 1:
                    tab.setText("멤버");
                    break;
                case 2:
                    tab.setText("활동");
                    break;
            }
        }).attach();
    }

    private void setupClickListeners() {
        fabAddTodo.setOnClickListener(v -> {
            CreateCollaborationTodoDialogFragment dialog =
                    CreateCollaborationTodoDialogFragment.newInstance(projectId);
            dialog.show(requireActivity().getSupportFragmentManager(), "CreateTodoDialog");
        });
    }

    private void observeData() {
        viewModel.getProjectById(projectId).observe(getViewLifecycleOwner(), project -> {
            if (project != null) {
                textProjectName.setText(project.getName());

                if (project.getDescription() != null && !project.getDescription().isEmpty()) {
                    textProjectDescription.setText(project.getDescription());
                    textProjectDescription.setVisibility(View.VISIBLE);
                } else {
                    textProjectDescription.setVisibility(View.GONE);
                }

                // 액션바 제목 설정
                if (getActivity() != null) {
                    getActivity().setTitle(project.getName());
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.project_detail_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_invite_member) {
            InviteMemberDialogFragment dialog = InviteMemberDialogFragment.newInstance(projectId);
            dialog.show(requireActivity().getSupportFragmentManager(), "InviteMemberDialog");
            return true;
        } else if (itemId == R.id.action_project_settings) {
            // 프로젝트 설정 화면으로 이동
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}