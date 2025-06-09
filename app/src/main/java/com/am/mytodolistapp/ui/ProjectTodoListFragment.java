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

public class ProjectTodoListFragment extends Fragment {

    private static final String ARG_PROJECT_ID = "project_id";

    private String projectId;
    private RecyclerView recyclerViewTodos;
    private TextView textEmptyMessage;
    private CollaborationTodoAdapter todoAdapter;
    private CollaborationViewModel viewModel;

    public static ProjectTodoListFragment newInstance(String projectId) {
        ProjectTodoListFragment fragment = new ProjectTodoListFragment();
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
        return inflater.inflate(R.layout.fragment_project_todo_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        observeData();
    }

    private void initViews(View view) {
        recyclerViewTodos = view.findViewById(R.id.recycler_view_todos);
        textEmptyMessage = view.findViewById(R.id.text_empty_message);
    }

    private void setupRecyclerView() {
        recyclerViewTodos.setLayoutManager(new LinearLayoutManager(getContext()));
        todoAdapter = new CollaborationTodoAdapter(viewModel);
        recyclerViewTodos.setAdapter(todoAdapter);
    }

    private void observeData() {
        viewModel.getProjectIncompleteTodos(projectId).observe(getViewLifecycleOwner(), todos -> {
            todoAdapter.submitList(todos);

            if (todos == null || todos.isEmpty()) {
                textEmptyMessage.setVisibility(View.VISIBLE);
                recyclerViewTodos.setVisibility(View.GONE);
            } else {
                textEmptyMessage.setVisibility(View.GONE);
                recyclerViewTodos.setVisibility(View.VISIBLE);
            }
        });
    }
}