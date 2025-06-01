package com.am.mytodolistapp.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CategoryItem;
import com.am.mytodolistapp.data.TodoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImprovedTaskListFragment extends Fragment {

    private TaskListViewModel taskListViewModel;
    private CategoryViewModel categoryViewModel;
    private RecyclerView recyclerViewCategoryFilter;
    private RecyclerView recyclerViewGroupedTasks;
    private CategoryFilterAdapter categoryFilterAdapter;
    private GroupedTaskAdapter groupedTaskAdapter;
    private FloatingActionButton fabAddTask;
    private ImageButton buttonVoiceAdd;

    // 현재 선택된 카테고리 필터 (null이면 모두)
    private Integer selectedCategoryId = null;

    // 음성 인식 관련
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        setupActivityResultLaunchers();
    }

    private void setupActivityResultLaunchers() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        startSpeechRecognition();
                    } else {
                        Toast.makeText(getContext(), "음성 인식을 사용하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    }
                });

        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            String spokenText = results.get(0).trim();
                            if (!spokenText.isEmpty()) {
                                TodoItem newItem = new TodoItem(spokenText);
                                taskListViewModel.insert(newItem);
                                Toast.makeText(getContext(), "'" + spokenText + "' 추가됨", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "인식된 내용이 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list_improved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerViews();
        setupClickListeners();
        observeData();
    }

    private void initViews(View view) {
        recyclerViewCategoryFilter = view.findViewById(R.id.recycler_view_category_filter);
        recyclerViewGroupedTasks = view.findViewById(R.id.recycler_view_grouped_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);
        buttonVoiceAdd = view.findViewById(R.id.button_voice_add);
    }

    private void setupRecyclerViews() {
        // 카테고리 필터 RecyclerView 설정
        recyclerViewCategoryFilter.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryFilterAdapter = new CategoryFilterAdapter((filterItem, position) -> {
            selectedCategoryId = filterItem.getCategoryId();
            Log.d("TaskListFilter", "선택된 카테고리 ID: " + selectedCategoryId + ", 이름: " + filterItem.getName());
            updateTaskFilter();
        });
        recyclerViewCategoryFilter.setAdapter(categoryFilterAdapter);

        // 그룹화된 할일 목록 RecyclerView 설정
        recyclerViewGroupedTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        groupedTaskAdapter = new GroupedTaskAdapter(taskListViewModel);
        recyclerViewGroupedTasks.setAdapter(groupedTaskAdapter);

        setupSwipeToDelete();
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 그룹 헤더는 삭제할 수 없으므로 여기서는 구현하지 않음
                // 개별 할일 삭제는 TaskWithDateAdapter에서 처리
            }
        }).attachToRecyclerView(recyclerViewGroupedTasks);
    }

    private void setupClickListeners() {
        // 할일 추가 FAB 클릭
        fabAddTask.setOnClickListener(v -> {
            AddTodoDialogFragment dialogFragment = new AddTodoDialogFragment();
            dialogFragment.show(requireActivity().getSupportFragmentManager(), "AddTodoDialog");
        });

        // 음성 추가 버튼 클릭
        buttonVoiceAdd.setOnClickListener(v -> checkPermissionAndStartRecognition());
    }

    private void observeData() {
        // 카테고리 목록 관찰하여 필터 업데이트
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            updateCategoryFilter(categories);
        });

        // *** 수정된 부분: 필터링된 할일 목록 관찰 ***
        taskListViewModel.getAllTodosWithCategory().observe(getViewLifecycleOwner(), todos -> {
            updateGroupedTasks(todos);
        });
    }

    private void updateCategoryFilter(List<CategoryItem> categories) {
        List<CategoryFilterAdapter.FilterItem> filterItems = new ArrayList<>();

        // "모두" 항목 추가
        filterItems.add(new CategoryFilterAdapter.FilterItem("모두", null, null));

        // 카테고리별 항목 추가
        for (CategoryItem category : categories) {
            filterItems.add(new CategoryFilterAdapter.FilterItem(
                    category.getName(), category.getColor(), category.getId()));
        }

        categoryFilterAdapter.submitList(filterItems);
    }

    private void updateTaskFilter() {
        if (selectedCategoryId == null) {
            taskListViewModel.showAllTodos();
        } else if (selectedCategoryId == 0) {
            taskListViewModel.showTodosWithoutCategory();
        } else {
            taskListViewModel.showTodosByCategory(selectedCategoryId);
        }
    }

    private void updateGroupedTasks(List<TaskListViewModel.TodoWithCategory> todos) {
        List<GroupedTaskAdapter.TaskGroup> groups = groupTodosByDate(todos);
        groupedTaskAdapter.submitList(groups);
    }

    private List<GroupedTaskAdapter.TaskGroup> groupTodosByDate(List<TaskListViewModel.TodoWithCategory> todos) {
        List<GroupedTaskAdapter.TaskGroup> groups = new ArrayList<>();

        List<TaskListViewModel.TodoWithCategory> previousTodos = new ArrayList<>();
        List<TaskListViewModel.TodoWithCategory> todayTodos = new ArrayList<>();
        List<TaskListViewModel.TodoWithCategory> futureTodos = new ArrayList<>();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTime(today.getTime());
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        for (TaskListViewModel.TodoWithCategory todoWithCategory : todos) {
            TodoItem todo = todoWithCategory.getTodoItem();

            if (todo.getDueDate() == null) {
                // 기한이 없는 할일은 "오늘"에 표시
                todayTodos.add(todoWithCategory);
            } else {
                Date dueDate = new Date(todo.getDueDate());

                if (dueDate.before(today.getTime())) {
                    previousTodos.add(todoWithCategory);
                } else if (dueDate.before(tomorrow.getTime())) {
                    todayTodos.add(todoWithCategory);
                } else {
                    futureTodos.add(todoWithCategory);
                }
            }
        }

        // 그룹에 할일이 있는 경우만 추가
        if (!previousTodos.isEmpty()) {
            groups.add(new GroupedTaskAdapter.TaskGroup("previous", "이전의", previousTodos));
        }
        if (!todayTodos.isEmpty()) {
            groups.add(new GroupedTaskAdapter.TaskGroup("today", "오늘", todayTodos));
        }
        if (!futureTodos.isEmpty()) {
            groups.add(new GroupedTaskAdapter.TaskGroup("future", "미래", futureTodos));
        }

        return groups;
    }

    // 음성 인식 관련 메소드들
    private void checkPermissionAndStartRecognition() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "할 일을 말하세요...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("ImprovedTaskListFragment", "음성 인식을 시작할 수 없습니다.", e);
            Toast.makeText(getContext(), "음성 인식을 지원하지 않거나 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }
}