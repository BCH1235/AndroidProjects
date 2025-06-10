package com.am.mytodolistapp.ui.collaboration;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.am.mytodolistapp.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddProjectTaskDialogFragment extends DialogFragment {

    private EditText editTaskTitle;
    private EditText editTaskContent;
    private CheckBox checkBoxSetDueDate;
    private TextView textSelectedDate;
    private Button buttonSelectDate;
    private Spinner spinnerPriority;
    private OnTaskAddedListener listener;

    private Calendar selectedDueDate = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN);

    public interface OnTaskAddedListener {
        void onTaskAdded(String title, String content, Long dueDate);
    }

    public void setOnTaskAddedListener(OnTaskAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_project_task, null);

        initViews(view);
        setupPrioritySpinner();
        setupDatePicker();
        setupClickListeners(view);

        builder.setView(view);
        return builder.create();
    }

    private void initViews(View view) {
        editTaskTitle = view.findViewById(R.id.edit_task_title);
        editTaskContent = view.findViewById(R.id.edit_task_content);
        checkBoxSetDueDate = view.findViewById(R.id.checkbox_set_due_date);
        textSelectedDate = view.findViewById(R.id.text_selected_date);
        buttonSelectDate = view.findViewById(R.id.button_select_date);
        spinnerPriority = view.findViewById(R.id.spinner_priority);
    }

    private void setupPrioritySpinner() {
        String[] priorities = {"낮음", "보통", "높음"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);
        spinnerPriority.setSelection(1); // 기본값: 보통
    }

    private void setupDatePicker() {
        textSelectedDate.setVisibility(View.GONE);
        buttonSelectDate.setVisibility(View.GONE);

        checkBoxSetDueDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                textSelectedDate.setVisibility(View.VISIBLE);
                buttonSelectDate.setVisibility(View.VISIBLE);
                if (selectedDueDate == null) {
                    selectedDueDate = Calendar.getInstance();
                    updateDateDisplay();
                }
            } else {
                textSelectedDate.setVisibility(View.GONE);
                buttonSelectDate.setVisibility(View.GONE);
                selectedDueDate = null;
            }
        });

        buttonSelectDate.setOnClickListener(v -> showDatePickerDialog());
    }

    private void setupClickListeners(View view) {
        Button buttonCancel = view.findViewById(R.id.button_cancel);
        Button buttonAdd = view.findViewById(R.id.button_add);

        buttonCancel.setOnClickListener(v -> dismiss());
        buttonAdd.setOnClickListener(v -> addTask());
    }

    private void showDatePickerDialog() {
        Calendar calendar = selectedDueDate != null ? selectedDueDate : Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDueDate.set(Calendar.MILLISECOND, 0);
                    updateDateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        if (selectedDueDate != null) {
            textSelectedDate.setText("기한: " + dateFormat.format(selectedDueDate.getTime()));
        }
    }

    private void addTask() {
        String title = editTaskTitle.getText().toString().trim();
        String content = editTaskContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "할 일 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Long dueDate = null;
        if (checkBoxSetDueDate.isChecked() && selectedDueDate != null) {
            dueDate = selectedDueDate.getTimeInMillis();
        }

        if (listener != null) {
            listener.onTaskAdded(title, content, dueDate);
        }
        dismiss();
    }
}
