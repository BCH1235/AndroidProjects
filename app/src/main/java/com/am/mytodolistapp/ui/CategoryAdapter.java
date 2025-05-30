package com.am.mytodolistapp.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CategoryItem;

public class CategoryAdapter extends ListAdapter<CategoryItem, CategoryAdapter.CategoryViewHolder> {

    private final CategoryViewModel viewModel;

    public CategoryAdapter(CategoryViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_category, parent, false);
        return new CategoryViewHolder(itemView, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final View viewCategoryColor;
        private final TextView textCategoryName;
        private final TextView textCategoryInfo;
        private final TextView textDefaultLabel;
        private final CategoryViewModel viewModel;

        public CategoryViewHolder(@NonNull View itemView, CategoryViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            viewCategoryColor = itemView.findViewById(R.id.view_category_color);
            textCategoryName = itemView.findViewById(R.id.text_category_name);
            textCategoryInfo = itemView.findViewById(R.id.text_category_info);
            textDefaultLabel = itemView.findViewById(R.id.text_default_label);

            // 카테고리 항목 클릭 시 편집
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CategoryItem category = ((CategoryAdapter)
                            ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);

                    EditCategoryDialogFragment dialog = EditCategoryDialogFragment.newInstance(category);
                    if (itemView.getContext() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                        dialog.show(activity.getSupportFragmentManager(), "EditCategoryDialog");
                    }
                }
            });
        }

        public void bind(CategoryItem category) {
            textCategoryName.setText(category.getName());

            // 카테고리 색상 설정
            try {
                int color = Color.parseColor(category.getColor());
                viewCategoryColor.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(color));
            } catch (Exception e) {
                // 기본 색상 사용
                viewCategoryColor.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FF6200EE")));
            }

            // 기본 카테고리 표시
            if (category.isDefault()) {
                textDefaultLabel.setVisibility(View.VISIBLE);
            } else {
                textDefaultLabel.setVisibility(View.GONE);
            }

            // 해당 카테고리를 사용하는 할 일 개수 표시
            viewModel.getTodoCountByCategory(category.getId(), count -> {
                if (itemView.getContext() instanceof AppCompatActivity) {
                    ((AppCompatActivity) itemView.getContext()).runOnUiThread(() -> {
                        textCategoryInfo.setText("할 일 " + count + "개");
                    });
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<CategoryItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<CategoryItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull CategoryItem oldItem, @NonNull CategoryItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull CategoryItem oldItem, @NonNull CategoryItem newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getColor().equals(newItem.getColor()) &&
                    oldItem.isDefault() == newItem.isDefault() &&
                    oldItem.getOrderIndex() == newItem.getOrderIndex();
        }
    };
}