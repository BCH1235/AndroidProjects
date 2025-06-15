package com.am.mytodolistapp.ui.task;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.am.mytodolistapp.data.CategoryItem;
import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.ui.category.CategoryViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

//카테고리 선택 기능 추가
public class AddTodoDialogFragment extends DialogFragment {

    private EditText editTextTodoTitle;
    private CheckBox checkBoxSetDueDate;
    private TextView textSelectedDate;
    private Button buttonSelectDate;
    //카테고리 선택을 위한 Spinner
    private Spinner spinnerCategory;
    private Button buttonCancel;
    private Button buttonAdd;
    private TaskListViewModel taskListViewModel;
    // CategoryViewModel
    private CategoryViewModel categoryViewModel;

    //카테고리 관련 변수들
    private List<CategoryItem> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    // 날짜 관련 변수들
    private Calendar selectedDueDate = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);
        //CategoryViewModel 초기화
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //카테고리 선택이 포함된 레이아웃 사용
        return inflater.inflate(R.layout.dialog_add_todo_with_date_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        //카테고리 스피너 설정
        setupCategorySpinner();
        setupDatePicker();
        setupClickListeners();

        editTextTodoTitle.requestFocus();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void initViews(View view) {
        editTextTodoTitle = view.findViewById(R.id.edit_text_todo_title);
        checkBoxSetDueDate = view.findViewById(R.id.checkbox_set_due_date);
        textSelectedDate = view.findViewById(R.id.text_selected_date);
        buttonSelectDate = view.findViewById(R.id.button_select_date);
        //스피너 찾기
        spinnerCategory = view.findViewById(R.id.spinner_category);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonAdd = view.findViewById(R.id.button_add);
    }

    //카테고리 스피너 설정
    private void setupCategorySpinner() {
        // 카테고리 목록을 위한 어댑터 설정
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("카테고리 없음"); // 첫 번째 항목은 "카테고리 없음"

        categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // 카테고리 목록 관찰
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            categoryList.clear();
            categoryList.addAll(categories);

            // 스피너 업데이트
            categoryNames.clear();
            categoryNames.add("카테고리 없음");
            for (CategoryItem category : categories) {
                categoryNames.add(category.getName());
            }
            categoryAdapter.notifyDataSetChanged();
        });
    }

    private void setupDatePicker() {
        // 기본적으로 날짜 선택 비활성화
        textSelectedDate.setVisibility(View.GONE);
        buttonSelectDate.setVisibility(View.GONE);

        // 체크박스 클릭 이벤트
        checkBoxSetDueDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                textSelectedDate.setVisibility(View.VISIBLE);
                buttonSelectDate.setVisibility(View.VISIBLE);
                if (selectedDueDate == null) {
                    // 기본값: 오늘 날짜
                    selectedDueDate = Calendar.getInstance();
                    updateDateDisplay();
                }
            } else {
                textSelectedDate.setVisibility(View.GONE);
                buttonSelectDate.setVisibility(View.GONE);
                selectedDueDate = null;
            }
        });

        // 날짜 선택 버튼 클릭
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

    private void setupClickListeners() {
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonAdd.setOnClickListener(v -> addTodo());
    }

    //카테고리 선택 로직 추가
    private void addTodo() {
        String todoTitle = editTextTodoTitle.getText().toString().trim();

        if (todoTitle.isEmpty()) {
            Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoItem newItem = new TodoItem(todoTitle);

        // 기한 날짜 설정
        if (checkBoxSetDueDate.isChecked() && selectedDueDate != null) {
            newItem.setDueDate(selectedDueDate.getTimeInMillis());
        }

        //선택된 카테고리 설정
        int selectedPosition = spinnerCategory.getSelectedItemPosition();
        if (selectedPosition > 0) { // 0은 "카테고리 없음"
            CategoryItem selectedCategory = categoryList.get(selectedPosition - 1);
            newItem.setCategoryId(selectedCategory.getId());
        }
        // selectedPosition이 0이면 categoryId는 null로 유지됨

        taskListViewModel.insert(newItem);
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