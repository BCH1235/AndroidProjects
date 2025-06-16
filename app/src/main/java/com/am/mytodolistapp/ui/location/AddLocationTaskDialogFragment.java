package com.am.mytodolistapp.ui.location;

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
import com.am.mytodolistapp.ui.category.CategoryViewModel;

import java.util.ArrayList;
import java.util.List;

// 특정 위치에 새로운 할 일을 추가하기 위한 UI를 제공하는 DialogFragment
// 사용자는 할 일의 제목과 카테고리를 선택하여, 현재 위치에 종속된 새로운 할 일을 생성할 수 있다.

/*  LocationTaskListFragment: 이 프래그먼트에서 호출되며, 생성된 TodoItem은 LocationBasedTaskViewModel을 통해 데이터베이스에 저장
    LocationBasedTaskViewModel: 할 일(TodoItem)의 추가, 수정, 삭제 등 데이터 처리를 담당
    CategoryViewModel: 카테고리 목록을 가져와 스피너(Spinner)에 표시하기 위해 상호작용한다 */
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

    //카테고리 목록을 표시할 드롭다운 메뉴를 설정
    //'카테고리 없음'을 기본
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


    //사용자가 입력한 정보로 새로운 위치 기반 할 일을 생성하고 저장
    private void addLocationTask() {
        String title = editTextTodoTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoItem newTodo = new TodoItem(title);
        newTodo.setLocationId(locationId);


        //지오펜스를 활성화합니다.
        newTodo.setLocationEnabled(true);

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