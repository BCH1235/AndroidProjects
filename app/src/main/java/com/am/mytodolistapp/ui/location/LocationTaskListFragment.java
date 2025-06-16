package com.am.mytodolistapp.ui.location;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class LocationTaskListFragment extends Fragment {

    private static final String ARG_LOCATION_ID = "location_id";
    private static final String ARG_LOCATION_NAME = "location_name";

    private int locationId;
    private String locationName;
    private RecyclerView recyclerViewTasks;
    private TextView textEmptyMessage;
    private FloatingActionButton fabAddTask;
    private LocationTaskAdapter adapter;
    private LocationBasedTaskViewModel viewModel;

    public static LocationTaskListFragment newInstance(int locationId, String locationName) {
        LocationTaskListFragment fragment = new LocationTaskListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LOCATION_ID, locationId);
        args.putString(ARG_LOCATION_NAME, locationName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            locationId = getArguments().getInt(ARG_LOCATION_ID);
            locationName = getArguments().getString(ARG_LOCATION_NAME);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(LocationBasedTaskViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTitle();
        setupRecyclerView();
        // [수정] 스와이프 삭제 기능 제거
        // setupSwipeToDelete();
        setupFabClickListener();
        observeData();
    }

    private void initViews(View view) {
        recyclerViewTasks = view.findViewById(R.id.recycler_view_location_tasks);
        textEmptyMessage = view.findViewById(R.id.text_empty_message);
        fabAddTask = view.findViewById(R.id.fab_add_location_task);
    }

    // ... (나머지 코드는 동일)

    private void setupTitle() {
        if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(locationName + " 할 일");
        }
    }

    private void setupRecyclerView() {
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LocationTaskAdapter(viewModel);
        recyclerViewTasks.setAdapter(adapter);
    }

    private void setupFabClickListener() {
        fabAddTask.setOnClickListener(v -> {
            AddLocationTaskDialogFragment dialog = AddLocationTaskDialogFragment.newInstance(locationId);
            dialog.show(requireActivity().getSupportFragmentManager(), "AddLocationTaskDialog");
        });
    }

    private void observeData() {
        viewModel.getTodosByLocationId(locationId).observe(getViewLifecycleOwner(), this::updateTaskList);
    }

    private void updateTaskList(List<TodoItem> todos) {
        adapter.submitList(todos);
        if (todos == null || todos.isEmpty()) {
            showEmptyState();
        } else {
            showTaskList();
        }
    }

    private void showEmptyState() {
        textEmptyMessage.setVisibility(View.VISIBLE);
        recyclerViewTasks.setVisibility(View.GONE);
        textEmptyMessage.setText("이 위치에 등록된 할 일이 없습니다.\n\n" +
                "'+' 버튼을 눌러 새로운 할 일을 추가해보세요!");
    }

    private void showTaskList() {
        textEmptyMessage.setVisibility(View.GONE);
        recyclerViewTasks.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupTitle();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        }
    }
}