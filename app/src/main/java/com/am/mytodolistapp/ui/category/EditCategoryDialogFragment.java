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


//기존 카테고리의 이름과 색상을 수정하기 위한 UI를 제공하는 DialogFragment
public class EditCategoryDialogFragment extends DialogFragment {

    // Bundle을 통해 전달받을 데이터의 키
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CATEGORY_COLOR = "category_color";
    private static final String ARG_IS_DEFAULT = "is_default";


    // UI 컴포넌트
    private EditText editCategoryName;
    private RecyclerView recyclerViewColors;
    private Button buttonCancel, buttonSave;


    // ViewModel 및 Adapter
    private CategoryViewModel viewModel;
    private ColorSelectionAdapter colorAdapter;


    // 상태 변수
    private String selectedColor;

    private int categoryId;
    private boolean isDefault; // 기본 카테고리 여부 (수정 불가)

    public static EditCategoryDialogFragment newInstance(CategoryItem category) {
        EditCategoryDialogFragment fragment = new EditCategoryDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORY_ID, category.getId());
        args.putString(ARG_CATEGORY_NAME, category.getName());
        args.putString(ARG_CATEGORY_COLOR, category.getColor());
        args.putBoolean(ARG_IS_DEFAULT, category.isDefault());
        fragment.setArguments(args);
        return fragment;
    }//  EditCategoryDialogFragment의 새 인스턴스를 생성하고, 수정할 카테고리 정보를 Bundle에 담아 전달한다.

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // Bundle로부터 전달받은 카테고리 정보를 멤버 변수에 저장
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


    //취소' 및 '저장' 버튼의 클릭 이벤트를 설정
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

        // 업데이트할 CategoryItem 객체를 생성
        CategoryItem updatedCategory = new CategoryItem();
        updatedCategory.setId(categoryId);
        updatedCategory.setName(name);
        updatedCategory.setColor(selectedColor);
        updatedCategory.setDefault(isDefault);

        // ViewModel을 통해 데이터베이스 업데이트를 요청한다
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