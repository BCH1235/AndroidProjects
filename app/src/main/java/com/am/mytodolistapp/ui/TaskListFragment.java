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

// 할 일 목록을 표시하고 관리하는 메인 화면
public class TaskListFragment extends Fragment {

    private TaskListViewModel taskListViewModel; // 데이터 및 로직 처리 ViewModel
    private RecyclerView recyclerView;           // 할 일 목록 표시 UI
    private TaskListAdapter adapter;             // RecyclerView 와 데이터를 연결하는 어댑터
    private FloatingActionButton fabAddTask;     // 할 일 추가 버튼 (플로팅 액션 버튼)
    private ImageButton buttonVoiceAdd;          // 음성으로 할 일 추가 버튼

    // 다른 Activity (음성 인식, 권한 요청) 로부터 결과를 받기 위한 콜백 핸들러들
    private ActivityResultLauncher<Intent> speechRecognizerLauncher; // 음성 인식 결과 처리
    private ActivityResultLauncher<String> requestPermissionLauncher;  // 권한 요청 결과 처리

    // Fragment 생성 시 초기화 (ViewModel 연결, ActivityResultLauncher 등록)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ViewModel 인스턴스 가져오기
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);

        // 마이크 권한 요청 결과 처리 설정
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startSpeechRecognition(); // 권한 승인 시 음성 인식 시작
            } else {
                Toast.makeText(getContext(), "음성 인식을 사용하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show(); // 권한 거부 시 알림
            }
        });

        // 음성 인식 Activity 결과 처리 설정
        speechRecognizerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                // 음성 인식 성공 시 결과 처리
                Intent data = result.getData();
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String spokenText = results.get(0).trim(); // 인식된 텍스트 추출
                    if (!spokenText.isEmpty()) {
                        TodoItem newItem = new TodoItem(spokenText); // 새 할 일 객체 생성
                        taskListViewModel.insert(newItem);           // ViewModel 에 삽입 요청
                        Toast.makeText(getContext(), "'" + spokenText + "' 추가됨", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "인식된 내용이 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                // 음성 인식 실패 또는 취소 시 처리 (필요 시)
            }
        });
    }

    // 화면의 레이아웃(XML)을 View 객체로 생성하고 기본 UI 설정
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 레이아웃 파일 로드
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);

        // RecyclerView 설정 (LayoutManager, Adapter 연결)
        recyclerView = view.findViewById(R.id.recycler_view_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskListAdapter(taskListViewModel); // 어댑터 생성 (ViewModel 전달)
        recyclerView.setAdapter(adapter);

        // 버튼들 찾기
        fabAddTask = view.findViewById(R.id.fab_add_task);
        buttonVoiceAdd = view.findViewById(R.id.button_voice_add);

        return view; // 생성된 View 반환
    }

    // View 가 완전히 생성된 후 UI 관련 로직 및 이벤트 리스너 설정
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        // 할 일 추가 FAB 클릭 이벤트 처리
        fabAddTask.setOnClickListener(v -> {
            // 할 일 추가 다이얼로그 표시
            AddTodoDialogFragment dialogFragment = new AddTodoDialogFragment();
            dialogFragment.show(requireActivity().getSupportFragmentManager(), "AddTodoDialog");
        });

        // 음성 추가 버튼 클릭 이벤트 처리
        buttonVoiceAdd.setOnClickListener(v -> {
            // 마이크 권한 확인 및 음성 인식 시작 로직 호출
            checkPermissionAndStartRecognition();
        });

        // RecyclerView 항목 스와이프로 삭제 기능 설정
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // 드래그 이동 미사용
            }

            // 항목이 스와이프 되었을 때 처리
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition(); // 스와이프된 항목 위치
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem swipedTodo = adapter.getCurrentList().get(position); // 스와이프된 항목 데이터
                    taskListViewModel.delete(swipedTodo); // ViewModel 에 삭제 요청

                    // 삭제 확인 및 '실행 취소' 기능 제공
                    Snackbar.make(recyclerView, "할 일이 삭제되었습니다.", Snackbar.LENGTH_LONG)
                            .setAction("실행 취소", vUndo -> taskListViewModel.insert(swipedTodo)) // 취소 시 다시 삽입
                            .show();
                }
            }
        }).attachToRecyclerView(recyclerView); // RecyclerView 에 스와이프 기능 연결

        // ViewModel 의 할 일 목록(LiveData) 데이터 변경 관찰 시작
        taskListViewModel.getAllTodos().observe(getViewLifecycleOwner(), todos -> {
            // 데이터가 변경될 때마다 어댑터에 새 목록 전달하여 UI 업데이트
            adapter.submitList(todos);
        });
    }

    // 마이크 권한 확인 및 음성 인식 시작 절차 진행
    private void checkPermissionAndStartRecognition() {
        // 권한 상태 확인
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // 권한 있으면 즉시 음성 인식 시작
            startSpeechRecognition();
        } else {
            // 권한 없으면 권한 요청 실행
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    // 시스템 음성 인식 Activity 시작
    private void startSpeechRecognition() {
        // 음성 인식을 위한 Intent 생성 및 설정
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); // 자유 형식
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); // 기본 언어
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "할 일을 말하세요..."); // 안내 문구

        try {
            // 음성 인식 Activity 시작
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            // 오류 처리 (음성 인식 미지원 등)
            Log.e("TaskListFragment", "음성 인식을 시작할 수 없습니다.", e);
            Toast.makeText(getContext(), "음성 인식을 지원하지 않거나 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

}