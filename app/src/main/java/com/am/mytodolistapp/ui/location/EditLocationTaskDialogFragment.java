package com.am.mytodolistapp.ui.location;

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

// 기존 위치 기반 할 일의 제목을 수정하기 위한 UI를 제공하는 DialogFragment
// 사용자는 이 다이얼로그를 통해 기존 할 일의 제목을 변경할 수 있다
public class EditLocationTaskDialogFragment extends DialogFragment {

    private static final String ARG_TODO_ID = "todo_id";
    private static final String ARG_TODO_TITLE = "todo_title";
    private static final String ARG_TODO_IS_COMPLETED = "todo_is_completed";
    private static final String ARG_LOCATION_ID = "location_id";

    private EditText editTextTodoTitle;
    private Button buttonCancel, buttonSave;
    private LocationBasedTaskViewModel viewModel;

    private int todoId;
    private boolean isCompleted;
    private int locationId;


    // 수정할 TodoItem 객체를 받아, 필요한 정보를 Bundle에 담아 프래그먼트 인스턴스를 생성
    public static EditLocationTaskDialogFragment newInstance(TodoItem todoItem) {
        EditLocationTaskDialogFragment fragment = new EditLocationTaskDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TODO_ID, todoItem.getId());
        args.putString(ARG_TODO_TITLE, todoItem.getTitle());
        args.putBoolean(ARG_TODO_IS_COMPLETED, todoItem.isCompleted());
        args.putInt(ARG_LOCATION_ID, todoItem.getLocationId());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LocationBasedTaskViewModel.class);

        // Bundle로부터 전달받은 할 일 정보를 멤버 변수에 저장합니다.
        if (getArguments() != null) {
            todoId = getArguments().getInt(ARG_TODO_ID);
            isCompleted = getArguments().getBoolean(ARG_TODO_IS_COMPLETED);
            locationId = getArguments().getInt(ARG_LOCATION_ID);
        } else {
            dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_todo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소 찾기
        editTextTodoTitle = view.findViewById(R.id.edit_text_todo_title_edit);
        buttonCancel = view.findViewById(R.id.button_cancel_edit);
        buttonSave = view.findViewById(R.id.button_save);

        if (getArguments() != null) {
            String currentTitle = getArguments().getString(ARG_TODO_TITLE);
            editTextTodoTitle.setText(currentTitle);
            editTextTodoTitle.setSelection(currentTitle != null ? currentTitle.length() : 0);
        }

        // 버튼 이벤트
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonSave.setOnClickListener(v -> saveLocationTask());

        editTextTodoTitle.requestFocus();
    }

    private void saveLocationTask() {
        String updatedTitle = editTextTodoTitle.getText().toString().trim();

        if (updatedTitle.isEmpty()) {
            Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🚨 새로운 객체를 만들지 않고, ViewModel에 ID와 새 제목만 전달합니다.
        viewModel.updateTodo(todoId, updatedTitle);
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}