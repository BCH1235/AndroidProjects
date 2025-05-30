package com.am.mytodolistapp.ui;

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

public class EditCategoryDialogFragment extends DialogFragment {

    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CATEGORY_COLOR = "category_color";
    private static final String ARG_IS_DEFAULT = "is_default";

    private EditText editCategoryName;
    private RecyclerView recyclerViewColors;
    private Button buttonCancel, buttonSave;
    private CategoryViewModel viewModel;
    private ColorSelectionAdapter colorAdapter;
    private String selectedColor;

    private int categoryId;
    private boolean isDefault;

    public static EditCategoryDialogFragment newInstance(CategoryItem category) {
        EditCategoryDialogFragment fragment = new EditCategoryDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORY_ID, category.getId());
        args.putString(ARG_CATEGORY_NAME, category.getName());
        args.putString(ARG_CATEGORY_COLOR, category.getColor());
        args.putBoolean(ARG_IS_DEFAULT, category.isDefault());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        if (getArguments() != null) {
            categoryId = getArguments().getInt(ARG_CATEGORY_ID);
            selectedColor = getArguments().getString(ARG_CATEGORY_COLOR, CategoryViewModel.PREDEFINED_COLORS[0]);
            isDefault = getArguments().getBoolean(ARG_IS_DEFAULT);
        } else {
            dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupColorSelection();
        setupClickListeners();
        loadCategoryData();

        editCategoryName.requestFocus();
    }

    private void initViews(View view) {
        editCategoryName = view.findViewById(R.id.edit_category_name);
        recyclerViewColors = view.findViewById(R.id.recycler_view_colors);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonSave = view.findViewById(R.id.button_save);
    }

    private void setupColorSelection() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewColors.setLayoutManager(layoutManager);

        colorAdapter = new ColorSelectionAdapter(CategoryViewModel.PREDEFINED_COLORS, color -> {
            selectedColor = color;
        });
        recyclerViewColors.setAdapter(colorAdapter);
        colorAdapter.setSelectedColor(selectedColor);
    }

    private void setupClickListeners() {
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonSave.setOnClickListener(v -> saveCategory());
    }

    private void loadCategoryData() {
        if (getArguments() != null) {
            String currentName = getArguments().getString(ARG_CATEGORY_NAME);
            editCategoryName.setText(currentName);
            editCategoryName.setSelection(currentName != null ? currentName.length() : 0);
        }
    }

    private void saveCategory() {
        String name = editCategoryName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "카테고리 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryItem updatedCategory = new CategoryItem();
        updatedCategory.setId(categoryId);
        updatedCategory.setName(name);
        updatedCategory.setColor(selectedColor);
        updatedCategory.setDefault(isDefault);

        viewModel.updateCategory(updatedCategory);
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