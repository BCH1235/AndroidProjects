package com.am.mytodolistapp.ui.collaboration;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.ProjectTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditProjectTaskDialogFragment extends DialogFragment {

    private static final String ARG_TASK = "task";

    private EditText editTaskTitle;
    private EditText editTaskContent;
    private CheckBox checkBoxSetDueDate;
    private TextView textSelectedDate;
    private Button buttonSelectDate;
    private Spinner spinnerPriority;
    private OnTaskUpdatedListener listener;

    private ProjectTask originalTask;
    private Calendar selectedDueDate = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN);

    public interface OnTaskUpdatedListener {
        void onTaskUpdated(ProjectTask task);
    }

    public static EditProjectTaskDialogFragment newInstance(ProjectTask task) {
        EditProjectTaskDialogFragment fragment = new EditProjectTaskDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK, task);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnTaskUpdatedListener(OnTaskUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            originalTask = (ProjectTask) getArguments().getSerializable(ARG_TASK);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_project_task, null);

        initViews(view);
        setupPrioritySpinner();
        setupDatePicker();
        setupClickListeners(view);
        loadTaskData();

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
    }

    private void setupDatePicker() {
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
        Button buttonSave = view.findViewById(R.id.button_save);

        buttonCancel.setOnClickListener(v -> dismiss());
        buttonSave.setOnClickListener(v -> saveTask());
    }

    private void loadTaskData() {
        if (originalTask != null) {
            editTaskTitle.setText(originalTask.getTitle());
            editTaskContent.setText(originalTask.getContent());

            // 우선순위 설정
            String priority = originalTask.getPriority();
            if (priority != null) {
                switch (priority) {
                    case "LOW":
                        spinnerPriority.setSelection(0);
                        break;
                    case "MEDIUM":
                        spinnerPriority.setSelection(1);
                        break;
                    case "HIGH":
                        spinnerPriority.setSelection(2);
                        break;
                }
            }

            // 기한 설정
            if (originalTask.getDueDate() != null) {
                selectedDueDate = Calendar.getInstance();
                selectedDueDate.setTimeInMillis(originalTask.getDueDate());
                checkBoxSetDueDate.setChecked(true);
                updateDateDisplay();
            }
        }
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

    private void saveTask() {
        String title = editTaskTitle.getText().toString().trim();
        String content = editTaskContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "할 일 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 기존 작업 복사
        ProjectTask updatedTask = new ProjectTask(
                originalTask.getTaskId(),
                originalTask.getProjectId(),
                title,
                originalTask.getCreatedBy()
        );

        updatedTask.setContent(content);
        updatedTask.setCompleted(originalTask.isCompleted());
        updatedTask.setAssignedTo(originalTask.getAssignedTo());
        updatedTask.setCreatedAt(originalTask.getCreatedAt());

        // 우선순위 설정
        String[] priorityValues = {"LOW", "MEDIUM", "HIGH"};
        updatedTask.setPriority(priorityValues[spinnerPriority.getSelectedItemPosition()]);

        // 기한 설정
        if (checkBoxSetDueDate.isChecked() && selectedDueDate != null) {
            updatedTask.setDueDate(selectedDueDate.getTimeInMillis());
        } else {
            updatedTask.setDueDate(null);
        }

        if (listener != null) {
            listener.onTaskUpdated(updatedTask);
        }
        dismiss();
    }
}