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
import com.am.mytodolistapp.data.TodoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Locale;

public class TaskListFragment extends Fragment {

    private TaskListViewModel taskListViewModel;
    private CategoryViewModel categoryViewModel;
    private RecyclerView recyclerView;
    private TaskListAdapter adapter;
    private FloatingActionButton fabAddTask;
    private ImageButton buttonVoiceAdd;

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
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        observeData();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);
        buttonVoiceAdd = view.findViewById(R.id.button_voice_add);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskListAdapter(taskListViewModel);
        recyclerView.setAdapter(adapter);
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
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TaskListViewModel.TodoWithCategory swipedTodoWithCategory = adapter.getCurrentList().get(position);
                    TodoItem swipedTodo = swipedTodoWithCategory.getTodoItem();
                    taskListViewModel.delete(swipedTodo);

                    Snackbar.make(recyclerView, "할 일이 삭제되었습니다.", Snackbar.LENGTH_LONG)
                            .setAction("실행 취소", vUndo -> taskListViewModel.insert(swipedTodo))
                            .show();
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void setupClickListeners() {
        // 할 일 추가 FAB 클릭
        fabAddTask.setOnClickListener(v -> {
            AddTodoDialogFragment dialogFragment = new AddTodoDialogFragment();
            dialogFragment.show(requireActivity().getSupportFragmentManager(), "AddTodoDialog");
        });

        // 음성 추가 버튼 클릭
        buttonVoiceAdd.setOnClickListener(v -> checkPermissionAndStartRecognition());
    }

    private void observeData() {
        // 카테고리 정보와 함께 할 일 목록 관찰
        taskListViewModel.getAllTodosWithCategory().observe(getViewLifecycleOwner(), todos -> {
            adapter.submitList(todos);
        });
    }

    // 음성 인식 메소드들
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
            Log.e("TaskListFragment", "음성 인식을 시작할 수 없습니다.", e);
            Toast.makeText(getContext(), "음성 인식을 지원하지 않거나 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }
}