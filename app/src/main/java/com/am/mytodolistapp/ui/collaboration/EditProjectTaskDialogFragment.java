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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.ProjectTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

// 기존 협업 프로젝트 할 일을 수정하기 위한 UI를 제공하는 DialogFragment
// 사용자는 할 일의 제목, 내용, 마감 기한을 수정할 수 있다.
public class EditProjectTaskDialogFragment extends DialogFragment {

    private static final String ARG_TASK = "task";

    private EditText editTaskTitle;
    private EditText editTaskContent;
    private CheckBox checkBoxSetDueDate;
    private TextView textSelectedDate;
    private Button buttonSelectDate;
    // private Spinner spinnerPriority; // [삭제]
    private OnTaskUpdatedListener listener;

    private ProjectTask originalTask;
    private Calendar selectedDueDate = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN);

    public interface OnTaskUpdatedListener {
        void onTaskUpdated(ProjectTask task);
    }


    //  EditProjectTaskDialogFragment의 새 인스턴스를 생성하고, 수정할 할 일 정보를 Bundle에 담아 전달한다.
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

    }



    private void setupDatePicker() {
        checkBoxSetDueDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textSelectedDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            buttonSelectDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && selectedDueDate == null) {
                selectedDueDate = Calendar.getInstance();
                updateDateDisplay();
            } else if (!isChecked) {
                selectedDueDate = null;
            }
        });

        buttonSelectDate.setOnClickListener(v -> showDatePickerDialog());
    }// 마감 기한 설정 관련 UI를 설정

    private void setupClickListeners(View view) {
        Button buttonCancel = view.findViewById(R.id.button_cancel);
        Button buttonSave = view.findViewById(R.id.button_save);

        buttonCancel.setOnClickListener(v -> dismiss());
        buttonSave.setOnClickListener(v -> saveTask());
    } // '취소' 및 '저장' 버튼의 클릭 이벤트를 설정

    private void loadTaskData() {
        if (originalTask != null) {
            editTaskTitle.setText(originalTask.getTitle());
            editTaskContent.setText(originalTask.getContent());

            if (originalTask.getDueDate() != null) {
                selectedDueDate = Calendar.getInstance();
                selectedDueDate.setTimeInMillis(originalTask.getDueDate());
                checkBoxSetDueDate.setChecked(true);
                updateDateDisplay();
            }
        }
    } // 전달받은 기존 할 일 데이터를 UI에 로드

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
    } // DatePickerDialog를 생성하고 표시

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