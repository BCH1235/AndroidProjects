package com.am.mytodolistapp.ui.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CategoryItem;

public class AddCategoryDialogFragment extends DialogFragment {

    private EditText editCategoryName;
    private RecyclerView recyclerViewColors;
    private Button buttonCancel, buttonAdd;
    private CategoryViewModel viewModel;
    private ColorSelectionAdapter colorAdapter;
    private String selectedColor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        selectedColor = CategoryViewModel.PREDEFINED_COLORS[0]; // 기본 선택 색상
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupColorSelection();
        setupClickListeners();

        editCategoryName.requestFocus();
    }

    private void initViews(View view) {
        editCategoryName = view.findViewById(R.id.edit_category_name);
        recyclerViewColors = view.findViewById(R.id.recycler_view_colors);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonAdd = view.findViewById(R.id.button_add);
    }

    private void setupColorSelection() {
        // 가로 스크롤 가능한 색상 선택 RecyclerView 설정
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewColors.setLayoutManager(layoutManager);

        colorAdapter = new ColorSelectionAdapter(CategoryViewModel.PREDEFINED_COLORS, color -> {
            selectedColor = color;
        });
        recyclerViewColors.setAdapter(colorAdapter);

        // 첫 번째 색상을 기본 선택
        colorAdapter.setSelectedColor(selectedColor);
    }

    private void setupClickListeners() {
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonAdd.setOnClickListener(v -> addCategory());
    }

    private void addCategory() {
        String name = editCategoryName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "카테고리 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryItem newCategory = new CategoryItem(name, selectedColor);
        viewModel.insertCategory(newCategory);
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