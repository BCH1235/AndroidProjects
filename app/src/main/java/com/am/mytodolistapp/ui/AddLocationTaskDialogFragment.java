package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

public class AddLocationTaskDialogFragment extends DialogFragment {

    private static final String ARG_LOCATION_ID = "location_id";

    private EditText editTextTodoTitle;
    private NumberPicker numberPickerHour, numberPickerMinute;
    private Button buttonCancel, buttonAdd;
    private LocationBasedTaskViewModel viewModel;
    private int locationId;

    public static AddLocationTaskDialogFragment newInstance(int locationId) {
        AddLocationTaskDialogFragment fragment = new AddLocationTaskDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LOCATION_ID, locationId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LocationBasedTaskViewModel.class);

        if (getArguments() != null) {
            locationId = getArguments().getInt(ARG_LOCATION_ID);
        } else {
            dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_todo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소 찾기
        editTextTodoTitle = view.findViewById(R.id.edit_text_todo_title);
        numberPickerHour = view.findViewById(R.id.number_picker_hour_add);
        numberPickerMinute = view.findViewById(R.id.number_picker_minute_add);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonAdd = view.findViewById(R.id.button_add);

        // NumberPicker 설정
        numberPickerHour.setMinValue(0);
        numberPickerHour.setMaxValue(23);
        numberPickerHour.setValue(0);

        numberPickerMinute.setMinValue(0);
        numberPickerMinute.setMaxValue(59);
        numberPickerMinute.setValue(0);

        // 버튼 이벤트
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonAdd.setOnClickListener(v -> addLocationTask());

        // 자동 포커스 및 키보드
        editTextTodoTitle.requestFocus();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void addLocationTask() {
        numberPickerHour.clearFocus();
        numberPickerMinute.clearFocus();

        String title = editTextTodoTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour = numberPickerHour.getValue();
        int minute = numberPickerMinute.getValue();
        int estimatedTime = (hour * 60) + minute;

        TodoItem newTodo = new TodoItem(title);
        newTodo.setEstimatedTimeMinutes(estimatedTime);
        newTodo.setLocationId(locationId); // 위치 ID 설정

        viewModel.insertTodo(newTodo);
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