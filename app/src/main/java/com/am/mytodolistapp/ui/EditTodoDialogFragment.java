package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CategoryItem;
import com.am.mytodolistapp.data.TodoItem;

import java.util.ArrayList;
import java.util.List;


public class EditTodoDialogFragment extends DialogFragment {

    private EditText editTextTodoTitleEdit;

    private Spinner spinnerCategory;
    private Button buttonCancelEdit;
    private Button buttonSave;
    private TaskListViewModel taskListViewModel;

    private CategoryViewModel categoryViewModel;

    // 수정할 할 일의 기존 데이터를 전달받기 위한 키 및 변수
    private static final String ARG_TODO_ID = "todo_id";
    private static final String ARG_TODO_TITLE = "todo_title";
    private static final String ARG_TODO_IS_COMPLETED = "todo_is_completed";
    private static final String ARG_TODO_CATEGORY_ID = "todo_category_id";
    private int todoId;
    private boolean isCompleted;
    private Integer currentCategoryId;

    // 카테고리 관련 변수들
    private List<CategoryItem> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    public static EditTodoDialogFragment newInstance(TodoItem todoItem) {
        EditTodoDialogFragment fragment = new EditTodoDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TODO_ID, todoItem.getId());
        args.putString(ARG_TODO_TITLE, todoItem.getTitle());
        args.putBoolean(ARG_TODO_IS_COMPLETED, todoItem.isCompleted());
        // 카테고리 ID 전달
        if (todoItem.getCategoryId() != null) {
            args.putInt(ARG_TODO_CATEGORY_ID, todoItem.getCategoryId());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);
        //CategoryViewModel 초기화
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        if (getArguments() != null) {
            todoId = getArguments().getInt(ARG_TODO_ID);
            isCompleted = getArguments().getBoolean(ARG_TODO_IS_COMPLETED);
            //현재 카테고리 ID 읽기
            if (getArguments().containsKey(ARG_TODO_CATEGORY_ID)) {
                currentCategoryId = getArguments().getInt(ARG_TODO_CATEGORY_ID);
            }
        } else {
            dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //카테고리 선택이 포함된 레이아웃 사용
        return inflater.inflate(R.layout.dialog_edit_todo_with_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        // 카테고리 스피너 설정
        setupCategorySpinner();
        setupClickListeners();
        loadTodoData();

        editTextTodoTitleEdit.requestFocus();
    }

    private void initViews(View view) {
        editTextTodoTitleEdit = view.findViewById(R.id.edit_text_todo_title_edit);
        //스피너 찾기
        spinnerCategory = view.findViewById(R.id.spinner_category);
        buttonCancelEdit = view.findViewById(R.id.button_cancel_edit);
        buttonSave = view.findViewById(R.id.button_save);
    }

    //카테고리 스피너 설정
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

            //현재 카테고리 선택
            selectCurrentCategory();
        });
    }

    //현재 카테고리 선택
    private void selectCurrentCategory() {
        if (currentCategoryId != null) {
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).getId() == currentCategoryId) {
                    spinnerCategory.setSelection(i + 1); // +1은 "카테고리 없음" 때문
                    break;
                }
            }
        } else {
            spinnerCategory.setSelection(0); // "카테고리 없음" 선택
        }
    }

    private void setupClickListeners() {
        buttonCancelEdit.setOnClickListener(v -> dismiss());
        buttonSave.setOnClickListener(v -> saveTodo());
    }

    private void loadTodoData() {
        if (getArguments() != null) {
            String currentTitle = getArguments().getString(ARG_TODO_TITLE);
            editTextTodoTitleEdit.setText(currentTitle);
            editTextTodoTitleEdit.setSelection(currentTitle != null ? currentTitle.length() : 0);
        }
    }

    //카테고리 선택 로직 추가
    private void saveTodo() {
        String updatedTitle = editTextTodoTitleEdit.getText().toString().trim();

        if (updatedTitle.isEmpty()) {
            Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoItem updatedItem = new TodoItem();
        updatedItem.setId(todoId);
        updatedItem.setTitle(updatedTitle);
        updatedItem.setCompleted(isCompleted);

        //선택된 카테고리 설정
        int selectedPosition = spinnerCategory.getSelectedItemPosition();
        if (selectedPosition > 0) { // 0은 "카테고리 없음"
            CategoryItem selectedCategory = categoryList.get(selectedPosition - 1);
            updatedItem.setCategoryId(selectedCategory.getId());
        } else {
            updatedItem.setCategoryId(null); // 카테고리 없음
        }

        taskListViewModel.update(updatedItem);
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