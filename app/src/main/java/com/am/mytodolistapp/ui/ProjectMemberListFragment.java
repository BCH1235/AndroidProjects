package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;

public class ProjectMemberListFragment extends Fragment {

    private static final String ARG_PROJECT_ID = "project_id";

    private String projectId;
    private RecyclerView recyclerViewMembers;
    private ProjectMemberAdapter memberAdapter;
    private CollaborationViewModel viewModel;

    public static ProjectMemberListFragment newInstance(String projectId) {
        ProjectMemberListFragment fragment = new ProjectMemberListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            projectId = getArguments().getString(ARG_PROJECT_ID);
        }

        viewModel = new ViewModelProvider(requireActivity()).get(CollaborationViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_project_member_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        observeData();
    }

    private void initViews(View view) {
        recyclerViewMembers = view.findViewById(R.id.recycler_view_members);
    }

    private void setupRecyclerView() {
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        memberAdapter = new ProjectMemberAdapter();
        recyclerViewMembers.setAdapter(memberAdapter);
    }

    private void observeData() {
        viewModel.getProjectMembers(projectId).observe(getViewLifecycleOwner(), members -> {
            memberAdapter.submitList(members);
        });
    }
}