package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CategoryItem;
import com.am.mytodolistapp.data.TodoItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTodoWithDateDialogFragment extends DialogFragment {

    private static final String ARG_SELECTED_DATE = "selected_date";

    private EditText editTextTodoTitle;
    private TextView textSelectedDate;
    private Spinner spinnerCategory;
    private Button buttonCancel, buttonAdd;
    private TaskListViewModel taskListViewModel;
    private CategoryViewModel categoryViewModel;

    private LocalDate selectedDate;
    private List<CategoryItem> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    public static AddTodoWithDateDialogFragment newInstance(LocalDate selectedDate) {
        AddTodoWithDateDialogFragment fragment = new AddTodoWithDateDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_DATE, selectedDate.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        if (getArguments() != null) {
            String dateString = getArguments().getString(ARG_SELECTED_DATE);
            selectedDate = LocalDate.parse(dateString);
        } else {
            selectedDate = LocalDate.now();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_todo_with_date, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupCategorySpinner();
        setupDateDisplay();
        setupClickListeners();

        editTextTodoTitle.requestFocus();
    }

    private void initViews(View view) {
        editTextTodoTitle = view.findViewById(R.id.edit_text_todo_title);
        textSelectedDate = view.findViewById(R.id.text_selected_date);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonAdd = view.findViewById(R.id.button_add);
    }

    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("카테고리 없음");

        categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // 카테고리 목록 관찰
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            categoryList.clear();
            categoryList.addAll(categories);

            categoryNames.clear();
            categoryNames.add("카테고리 없음");
            for (CategoryItem category : categories) {
                categoryNames.add(category.getName());
            }
            categoryAdapter.notifyDataSetChanged();
        });
    }

    private void setupDateDisplay() {
        // 선택된 날짜를 표시
        String formattedDate = selectedDate.format(
                DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN));
        textSelectedDate.setText("기한: " + formattedDate);
    }

    private void setupClickListeners() {
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonAdd.setOnClickListener(v -> addTodoWithDate());
    }

    private void addTodoWithDate() {
        String todoTitle = editTextTodoTitle.getText().toString().trim();

        if (todoTitle.isEmpty()) {
            Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoItem newItem = new TodoItem(todoTitle);

        // 선택된 날짜를 기한으로 설정 (해당 날짜의 00:00:00)
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        newItem.setDueDate(calendar.getTimeInMillis());

        // 선택된 카테고리 설정
        int selectedPosition = spinnerCategory.getSelectedItemPosition();
        if (selectedPosition > 0) { // 0은 "카테고리 없음"
            CategoryItem selectedCategory = categoryList.get(selectedPosition - 1);
            newItem.setCategoryId(selectedCategory.getId());
        }

        taskListViewModel.insert(newItem);
        Toast.makeText(getContext(), "할일이 추가되었습니다.", Toast.LENGTH_SHORT).show();
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