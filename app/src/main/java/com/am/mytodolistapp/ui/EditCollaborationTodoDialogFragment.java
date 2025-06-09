package com.am.mytodolistapp.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CollaborationTodoItem;
import com.am.mytodolistapp.data.ProjectMember;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditCollaborationTodoDialogFragment extends DialogFragment {

    private static final String ARG_TODO = "todo_item";

    private CollaborationTodoItem todoItem;
    private EditText editTodoTitle;
    private EditText editTodoContent;
    private Spinner spinnerPriority;
    private Spinner spinnerAssignee;
    private CheckBox checkBoxSetDueDate;
    private TextView textSelectedDate;
    private Button buttonSelectDate;
    private Button buttonCancel, buttonSave, buttonDelete;
    private CollaborationViewModel viewModel;

    private Calendar selectedDueDate = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN);
    private List<ProjectMember> projectMembers = new ArrayList<>();

    public static EditCollaborationTodoDialogFragment newInstance(CollaborationTodoItem todo) {
        EditCollaborationTodoDialogFragment fragment = new EditCollaborationTodoDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TODO, todo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CollaborationViewModel.class);

        if (getArguments() != null) {
            todoItem = (CollaborationTodoItem) getArguments().getSerializable(ARG_TODO);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_collaboration_todo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupSpinners();
        setupDatePicker();
        setupClickListeners();
        loadProjectMembers();
        loadTodoData();

        editTodoTitle.requestFocus();
    }

    private void initViews(View view) {
        editTodoTitle = view.findViewById(R.id.edit_todo_title);
        editTodoContent = view.findViewById(R.id.edit_todo_content);
        spinnerPriority = view.findViewById(R.id.spinner_priority);
        spinnerAssignee = view.findViewById(R.id.spinner_assignee);
        checkBoxSetDueDate = view.findViewById(R.id.checkbox_set_due_date);
        textSelectedDate = view.findViewById(R.id.text_selected_date);
        buttonSelectDate = view.findViewById(R.id.button_select_date);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonSave = view.findViewById(R.id.button_save);
        buttonDelete = view.findViewById(R.id.button_delete);
    }

    private void setupSpinners() {
        // 우선순위 스피너
        String[] priorities = {"낮음", "보통", "높음"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
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

    private void loadProjectMembers() {
        viewModel.getProjectMembers(todoItem.getProjectId()).observe(getViewLifecycleOwner(), members -> {
            projectMembers.clear();
            projectMembers.addAll(members);

            List<String> memberNames = new ArrayList<>();
            memberNames.add("담당자 없음");
            for (ProjectMember member : members) {
                memberNames.add(member.getUserName());
            }

            ArrayAdapter<String> assigneeAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, memberNames);
            assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAssignee.setAdapter(assigneeAdapter);

            // 현재 담당자 선택
            if (todoItem.isAssigned()) {
                for (int i = 0; i < projectMembers.size(); i++) {
                    if (projectMembers.get(i).getUserId().equals(todoItem.getAssignedToId())) {
                        spinnerAssignee.setSelection(i + 1);
                        break;
                    }
                }
            }
        });
    }

    private void loadTodoData() {
        editTodoTitle.setText(todoItem.getTitle());
        editTodoContent.setText(todoItem.getContent());
        spinnerPriority.setSelection(todoItem.getPriority() - 1);

        if (todoItem.getDueDate() != null) {
            selectedDueDate = Calendar.getInstance();
            selectedDueDate.setTimeInMillis(todoItem.getDueDate());
            checkBoxSetDueDate.setChecked(true);
            textSelectedDate.setVisibility(View.VISIBLE);
            buttonSelectDate.setVisibility(View.VISIBLE);
            updateDateDisplay();
        }
    }

    private void setupClickListeners() {
        buttonCancel.setOnClickListener(v -> dismiss());

        buttonSave.setOnClickListener(v -> {
            String title = editTodoTitle.getText().toString().trim();
            String content = editTodoContent.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "할일 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            todoItem.setTitle(title);
            todoItem.setContent(content);
            todoItem.setPriority(spinnerPriority.getSelectedItemPosition() + 1);

            // 담당자 설정
            int assigneePosition = spinnerAssignee.getSelectedItemPosition();
            if (assigneePosition > 0 && !projectMembers.isEmpty()) {
                ProjectMember assignee = projectMembers.get(assigneePosition - 1);
                todoItem.setAssignedToId(assignee.getUserId());
                todoItem.setAssignedToName(assignee.getUserName());
            } else {
                todoItem.setAssignedToId(null);
                todoItem.setAssignedToName(null);
            }

            // 기한 설정
            if (checkBoxSetDueDate.isChecked() && selectedDueDate != null) {
                todoItem.setDueDate(selectedDueDate.getTimeInMillis());
            } else {
                todoItem.setDueDate(null);
            }

            viewModel.updateTodo(todoItem);
            Toast.makeText(getContext(), "할일이 수정되었습니다.", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        buttonDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("할일 삭제")
                    .setMessage("이 할일을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        viewModel.deleteTodo(todoItem);
                        Toast.makeText(getContext(), "할일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}