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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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

        // UI 초기화
        initViews(view);

        // 제목 설정
        setupTitle();

        // RecyclerView 설정
        setupRecyclerView();

        // 스와이프 삭제 기능 설정
        setupSwipeToDelete();

        // FAB 클릭 리스너 설정
        setupFabClickListener();

        // 데이터 관찰 시작
        observeData();
    }

    private void initViews(View view) {
        recyclerViewTasks = view.findViewById(R.id.recycler_view_location_tasks);
        textEmptyMessage = view.findViewById(R.id.text_empty_message);
        fabAddTask = view.findViewById(R.id.fab_add_location_task);
    }

    private void setupTitle() {
        if (getActivity() != null) {
            getActivity().setTitle(locationName + " 할 일");
        }

        // ActionBar가 있는 경우 뒤로가기 버튼 활성화
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setTitle(locationName + " 할 일");
            }
        }
    }

    private void setupRecyclerView() {
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LocationTaskAdapter(viewModel);
        recyclerViewTasks.setAdapter(adapter);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // 드래그 이동은 지원하지 않음
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    List<TodoItem> currentList = adapter.getCurrentList();
                    if (position < currentList.size()) {
                        TodoItem swipedTodo = currentList.get(position);

                        // 할 일 삭제
                        viewModel.deleteTodo(swipedTodo);

                        // Snackbar로 실행 취소 옵션 제공
                        Snackbar.make(recyclerViewTasks,
                                        "\"" + swipedTodo.getTitle() + "\" 할 일이 삭제되었습니다",
                                        Snackbar.LENGTH_LONG)
                                .setAction("실행 취소", v -> {
                                    // 실행 취소 시 할 일 다시 추가
                                    viewModel.insertTodo(swipedTodo);
                                })
                                .setAnchorView(fabAddTask) // FAB와 겹치지 않도록 위치 조정
                                .show();
                    }
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                // 스와이프 중일 때 아이템의 배경색 변경 (선택사항)
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder != null) {
                    viewHolder.itemView.setAlpha(0.7f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // 스와이프가 끝났을 때 원래 상태로 복원
                viewHolder.itemView.setAlpha(1.0f);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewTasks);
    }

    private void setupFabClickListener() {
        fabAddTask.setOnClickListener(v -> {
            AddLocationTaskDialogFragment dialog = AddLocationTaskDialogFragment.newInstance(locationId);
            dialog.show(requireActivity().getSupportFragmentManager(), "AddLocationTaskDialog");
        });
    }

    private void observeData() {
        // 해당 위치의 할 일 목록 관찰
        viewModel.getTodosByLocationId(locationId).observe(getViewLifecycleOwner(), todos -> {
            updateTaskList(todos);
        });
    }

    private void updateTaskList(List<TodoItem> todos) {
        adapter.submitList(todos);

        // 빈 목록 처리
        if (todos == null || todos.isEmpty()) {
            showEmptyState();
        } else {
            showTaskList();
        }
    }

    private void showEmptyState() {
        textEmptyMessage.setVisibility(View.VISIBLE);
        recyclerViewTasks.setVisibility(View.GONE);

        // 빈 상태 메시지 개선
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
        // Fragment가 다시 보여질 때 제목 재설정
        setupTitle();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // ActionBar 제목을 원래대로 복원
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                activity.getSupportActionBar().setTitle(R.string.app_name);
            }
        }
    }
}