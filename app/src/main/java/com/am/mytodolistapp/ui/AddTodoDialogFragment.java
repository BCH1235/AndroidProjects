package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;

// 새로운 할 일을 추가하는 다이얼로그 창
public class AddTodoDialogFragment extends DialogFragment {

    private EditText editTextTodoTitle;// 할 일 제목 입력란
    private Button buttonCancel;// 취소 버튼
    private Button buttonAdd;// 추가 버튼
    private TaskListViewModel taskListViewModel;// 할 일 목록 ViewModel

    // 프래그먼트 생성 시 초기 설정 (ViewModel 연결)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ViewModel 가져오기
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);
    }

    // 다이얼로그의 레이아웃(XML)을 화면에 표시할 View 객체로 생성
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_todo, container, false);
    }

    // View 생성 후 UI 요소들 초기화 및 이벤트 리스너 설정
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소들 찾기
        editTextTodoTitle = view.findViewById(R.id.edit_text_todo_title);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonAdd = view.findViewById(R.id.button_add);

        // 취소 버튼 클릭 시: 다이얼로그 닫기
        buttonCancel.setOnClickListener(v -> {
            dismiss(); // 다이얼로그 닫기
        });

        // 추가 버튼 클릭 시: 입력값으로 새 할 일 생성 및 ViewModel 에 전달
        buttonAdd.setOnClickListener(v -> {
            // 입력된 할 일 제목 가져오기
            String todoTitle = editTextTodoTitle.getText().toString().trim();

            // 제목이 비어있지 않으면 처리
            if (!todoTitle.isEmpty()) {

                // 새 TodoItem 객체 생성 (제목과 예상 시간 설정)
                TodoItem newItem = new TodoItem(todoTitle);
                // ViewModel 에게 데이터 삽입 요청
                taskListViewModel.insert(newItem);
                dismiss();// 다이얼로그 닫기
            } else {
                // 제목 입력값이 비어있으면 사용자에게 알림
                Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });


        // 다이얼로그 표시 시 제목 입력란에 자동 포커스
        editTextTodoTitle.requestFocus();
        // 키보드 자동 표시
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
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