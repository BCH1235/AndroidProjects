package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;

public class EditLocationTaskDialogFragment extends DialogFragment {

    private static final String ARG_TODO_ID = "todo_id";
    private static final String ARG_TODO_TITLE = "todo_title";
    private static final String ARG_TODO_IS_COMPLETED = "todo_is_completed";
    private static final String ARG_TODO_ESTIMATED_TIME = "todo_estimated_time";
    private static final String ARG_LOCATION_ID = "location_id";

    private EditText editTextTodoTitle;
    private NumberPicker numberPickerHour, numberPickerMinute;
    private Button buttonCancel, buttonSave;
    private LocationBasedTaskViewModel viewModel;

    private int todoId;
    private boolean isCompleted;
    private int estimatedTimeMinutes;
    private int locationId;

    public static EditLocationTaskDialogFragment newInstance(TodoItem todoItem) {
        EditLocationTaskDialogFragment fragment = new EditLocationTaskDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TODO_ID, todoItem.getId());
        args.putString(ARG_TODO_TITLE, todoItem.getTitle());
        args.putBoolean(ARG_TODO_IS_COMPLETED, todoItem.isCompleted());
        args.putInt(ARG_TODO_ESTIMATED_TIME, todoItem.getEstimatedTimeMinutes());
        args.putInt(ARG_LOCATION_ID, todoItem.getLocationId());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LocationBasedTaskViewModel.class);

        if (getArguments() != null) {
            todoId = getArguments().getInt(ARG_TODO_ID);
            isCompleted = getArguments().getBoolean(ARG_TODO_IS_COMPLETED);
            estimatedTimeMinutes = getArguments().getInt(ARG_TODO_ESTIMATED_TIME);
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
        numberPickerHour = view.findViewById(R.id.number_picker_hour_edit);
        numberPickerMinute = view.findViewById(R.id.number_picker_minute_edit);
        buttonCancel = view.findViewById(R.id.button_cancel_edit);
        buttonSave = view.findViewById(R.id.button_save);

        // NumberPicker 설정
        numberPickerHour.setMinValue(0);
        numberPickerHour.setMaxValue(23);
        numberPickerMinute.setMinValue(0);
        numberPickerMinute.setMaxValue(59);

        // 기존 값 설정
        int hours = estimatedTimeMinutes / 60;
        int minutes = estimatedTimeMinutes % 60;
        numberPickerHour.setValue(hours);
        numberPickerMinute.setValue(minutes);

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
        numberPickerHour.clearFocus();
        numberPickerMinute.clearFocus();

        String updatedTitle = editTextTodoTitle.getText().toString().trim();

        if (updatedTitle.isEmpty()) {
            Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour = numberPickerHour.getValue();
        int minute = numberPickerMinute.getValue();
        int updatedEstimatedTime = (hour * 60) + minute;

        TodoItem updatedItem = new TodoItem();
        updatedItem.setId(todoId);
        updatedItem.setTitle(updatedTitle);
        updatedItem.setCompleted(isCompleted);
        updatedItem.setEstimatedTimeMinutes(updatedEstimatedTime);
        updatedItem.setLocationId(locationId);

        viewModel.updateTodo(updatedItem);
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