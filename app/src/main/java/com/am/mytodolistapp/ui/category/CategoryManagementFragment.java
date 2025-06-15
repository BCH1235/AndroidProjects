package com.am.mytodolistapp.ui.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CategoryItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class CategoryManagementFragment extends Fragment {

    private RecyclerView recyclerViewCategories;
    private CategoryAdapter categoryAdapter;
    private FloatingActionButton fabAddCategory;
    private CategoryViewModel categoryViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        observeCategories();
    }

    private void initViews(View view) {
        recyclerViewCategories = view.findViewById(R.id.recycler_view_categories);
        fabAddCategory = view.findViewById(R.id.fab_add_category);
    }

    private void setupRecyclerView() {
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapter = new CategoryAdapter(categoryViewModel);
        recyclerViewCategories.setAdapter(categoryAdapter);

        // 스와이프로 삭제 기능 추가
        setupSwipeToDelete();
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CategoryItem swipedCategory = categoryAdapter.getCurrentList().get(position);

                    // 기본 카테고리는 삭제 불가능하도록 확인
                    if (swipedCategory.isDefault()) {
                        categoryAdapter.notifyItemChanged(position); // 원래 위치로 복구
                        Snackbar.make(recyclerViewCategories, "기본 카테고리는 삭제할 수 없습니다.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    // 해당 카테고리를 사용하는 할 일이 있는지 확인
                    categoryViewModel.deleteCategoryWithCheck(swipedCategory, todoCount -> {
                        requireActivity().runOnUiThread(() -> {
                            if (todoCount > 0) {
                                categoryAdapter.notifyItemChanged(position); // 원래 위치로 복구
                                Snackbar.make(recyclerViewCategories,
                                        "이 카테고리를 사용하는 할 일이 " + todoCount + "개 있습니다. 먼저 해당 할 일들의 카테고리를 변경해주세요.",
                                        Snackbar.LENGTH_LONG).show();
                            } else {
                                // 삭제 처리
                                categoryViewModel.deleteCategory(swipedCategory);
                                Snackbar.make(recyclerViewCategories, "카테고리가 삭제되었습니다.", Snackbar.LENGTH_LONG)
                                        .setAction("실행 취소", v -> categoryViewModel.insertCategory(swipedCategory))
                                        .show();
                            }
                        });
                    });
                }
            }
        }).attachToRecyclerView(recyclerViewCategories);
    }

    private void setupClickListeners() {
        fabAddCategory.setOnClickListener(v -> {
            AddCategoryDialogFragment dialog = new AddCategoryDialogFragment();
            dialog.show(requireActivity().getSupportFragmentManager(), "AddCategoryDialog");
        });
    }

    private void observeCategories() {
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            categoryAdapter.submitList(categories);
        });
    }
}