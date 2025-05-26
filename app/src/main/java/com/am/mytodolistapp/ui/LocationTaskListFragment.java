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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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

        // UI 초기화
        recyclerViewTasks = view.findViewById(R.id.recycler_view_location_tasks);
        textEmptyMessage = view.findViewById(R.id.text_empty_message);
        fabAddTask = view.findViewById(R.id.fab_add_location_task);

        // 제목 설정
        if (getActivity() != null) {
            getActivity().setTitle(locationName + " 할 일");
        }

        // RecyclerView 설정
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LocationTaskAdapter(viewModel);
        recyclerViewTasks.setAdapter(adapter);

        // 스와이프 삭제 기능
        setupSwipeToDelete();

        // FAB 클릭 - 새 할 일 추가
        fabAddTask.setOnClickListener(v -> {
            AddLocationTaskDialogFragment dialog = AddLocationTaskDialogFragment.newInstance(locationId);
            dialog.show(requireActivity().getSupportFragmentManager(), "AddLocationTaskDialog");
        });

        // 해당 위치의 할 일 목록 관찰
        viewModel.getTodosByLocationId(locationId).observe(getViewLifecycleOwner(), todos -> {
            adapter.submitList(todos);

            // 빈 목록 처리
            if (todos == null || todos.isEmpty()) {
                textEmptyMessage.setVisibility(View.VISIBLE);
                recyclerViewTasks.setVisibility(View.GONE);
            } else {
                textEmptyMessage.setVisibility(View.GONE);
                recyclerViewTasks.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem swipedTodo = adapter.getCurrentList().get(position);
                    viewModel.deleteTodo(swipedTodo);

                    Snackbar.make(recyclerViewTasks, "할 일이 삭제되었습니다.", Snackbar.LENGTH_LONG)
                            .setAction("실행 취소", v -> viewModel.insertTodo(swipedTodo))
                            .show();
                }
            }
        }).attachToRecyclerView(recyclerViewTasks);
    }
}