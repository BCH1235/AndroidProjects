package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;

// 기존 할 일의 제목 및 예상 시간을 수정하는 다이얼로그 창
public class EditTodoDialogFragment extends DialogFragment {

    private EditText editTextTodoTitleEdit;         // 할 일 제목 수정 입력란
    private Button buttonCancelEdit;                // 취소 버튼
    private Button buttonSave;                      // 저장 버튼
    private TaskListViewModel taskListViewModel;    // 할 일 목록 ViewModel (데이터 처리 위임용)

    // 수정할 할 일의 기존 데이터를 전달받기 위한 키 및 변수
    private static final String ARG_TODO_ID = "todo_id";
    private static final String ARG_TODO_TITLE = "todo_title";
    private static final String ARG_TODO_IS_COMPLETED = "todo_is_completed";
    private int todoId;                     // 수정할 할 일의 ID
    private boolean isCompleted;            // 수정할 할 일의 기존 완료 상태

    // 다이얼로그 인스턴스 생성 및 수정할 할 일 데이터 전달
    public static EditTodoDialogFragment newInstance(TodoItem todoItem) {
        EditTodoDialogFragment fragment = new EditTodoDialogFragment();
        Bundle args = new Bundle();
        // Bundle 에 수정할 데이터 담기
        args.putInt(ARG_TODO_ID, todoItem.getId());
        args.putString(ARG_TODO_TITLE, todoItem.getTitle());
        args.putBoolean(ARG_TODO_IS_COMPLETED, todoItem.isCompleted());
        fragment.setArguments(args);
        return fragment;
    }

    // 프래그먼트 생성 시 초기 설정 (ViewModel 연결, 전달받은 데이터 읽기)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ViewModel 가져오기
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);

        // Bundle 에서 수정할 데이터 읽어오기
        if (getArguments() != null) {
            todoId = getArguments().getInt(ARG_TODO_ID);
            isCompleted = getArguments().getBoolean(ARG_TODO_IS_COMPLETED);
        } else {
            dismiss(); // 데이터 없으면 닫기
        }
    }

    // 다이얼로그의 레이아웃(XML)을 화면에 표시할 View 객체로 생성
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_todo, container, false);
    }

    // View 생성 후 UI 요소들 초기화, 기존 값 채우기 및 이벤트 리스너 설정
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소들 찾기
        editTextTodoTitleEdit = view.findViewById(R.id.edit_text_todo_title_edit);
        buttonCancelEdit = view.findViewById(R.id.button_cancel_edit);
        buttonSave = view.findViewById(R.id.button_save);

        // 기존 제목을 EditText 에 설정 및 커서 마지막으로 이동
        if (getArguments() != null) {
            String currentTitle = getArguments().getString(ARG_TODO_TITLE);
            editTextTodoTitleEdit.setText(currentTitle);
            editTextTodoTitleEdit.setSelection(currentTitle != null ? currentTitle.length() : 0);
        }

        // 취소 버튼 클릭 시: 다이얼로그 닫기
        buttonCancelEdit.setOnClickListener(v -> dismiss());

        // 저장 버튼 클릭 시: 수정된 내용으로 할 일 업데이트 요청
        buttonSave.setOnClickListener(v -> {

            // 수정된 제목 가져오기
            String updatedTitle = editTextTodoTitleEdit.getText().toString().trim();

            // 제목이 비어있지 않으면 처리
            if (!updatedTitle.isEmpty()) {

                // 수정된 내용으로 TodoItem 객체 생성 (ID 와 완료 상태는 기존 값 유지)
                TodoItem updatedItem = new TodoItem();
                updatedItem.setId(todoId);
                updatedItem.setTitle(updatedTitle);
                updatedItem.setCompleted(isCompleted); // 기존 완료 상태 유지

                // ViewModel 에 할 일 업데이트 요청
                taskListViewModel.update(updatedItem);
                dismiss(); // 다이얼로그 닫기
            } else {
                // 제목이 비어있으면 사용자에게 알림
                Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 다이얼로그 표시 시 제목 입력란에 자동 포커스
        editTextTodoTitleEdit.requestFocus();
    }

    // 다이얼로그 크기 조절
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}