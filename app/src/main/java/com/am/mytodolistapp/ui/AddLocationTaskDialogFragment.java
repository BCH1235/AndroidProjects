package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

public class AddLocationTaskDialogFragment extends DialogFragment {

    private static final String ARG_LOCATION_ID = "location_id";

    private EditText editTextTodoTitle;
    private Spinner spinnerCategory;
    private Button buttonCancel, buttonAdd;
    private LocationBasedTaskViewModel viewModel;
    private CategoryViewModel categoryViewModel;
    private int locationId;

    // 카테고리 관련 변수들
    private List<CategoryItem> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

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
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        if (getArguments() != null) {
            locationId = getArguments().getInt(ARG_LOCATION_ID);
        } else {
            dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_todo_with_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupCategorySpinner();
        setupClickListeners();

        editTextTodoTitle.requestFocus();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void initViews(View view) {
        editTextTodoTitle = view.findViewById(R.id.edit_text_todo_title);
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

    private void setupClickListeners() {
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonAdd.setOnClickListener(v -> addLocationTask());
    }

    private void addLocationTask() {
        String title = editTextTodoTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoItem newTodo = new TodoItem(title);
        newTodo.setLocationId(locationId);

        // 선택된 카테고리 설정
        int selectedPosition = spinnerCategory.getSelectedItemPosition();
        if (selectedPosition > 0) {
            CategoryItem selectedCategory = categoryList.get(selectedPosition - 1);
            newTodo.setCategoryId(selectedCategory.getId());
        }

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