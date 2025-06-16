package com.am.mytodolistapp.ui.category;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;

import java.util.Objects;


// 할 일 목록 화면에서 카테고리 필터링 UI를 위한 RecyclerView 어댑터
// 사용자가 필터를 선택하면 해당 선택 상태를 관리하고, 외부 리스너에 이벤트를 전달
public class CategoryFilterAdapter extends ListAdapter<CategoryFilterAdapter.FilterItem, CategoryFilterAdapter.FilterViewHolder> {

    private int selectedPosition = 0; // 기본값: "모두" 선택
    private OnFilterClickListener listener; // 필터 클릭 이벤트를 전달할 리스너

    public interface OnFilterClickListener {
        void onFilterClick(FilterItem filterItem, int position);
    }// 필터 클릭 이벤트를 위한 인터페이스

    public CategoryFilterAdapter(OnFilterClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition); // 이전 선택 항목 UI 갱신
        notifyItemChanged(selectedPosition); // 새 선택 항목 UI 갱신
    }

    @NonNull
    @Override
    public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_filter, parent, false);
        return new FilterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        FilterItem item = getItem(position);
        holder.bind(item, position == selectedPosition);
    }


    // 각 필터 아이템의 뷰를 관리하는 ViewHolder 클래스.
    class FilterViewHolder extends RecyclerView.ViewHolder {
        private final View viewCategoryColor;
        private final TextView textCategoryName;

        public FilterViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryColor = itemView.findViewById(R.id.view_category_color);
            textCategoryName = itemView.findViewById(R.id.text_category_name);

            // 필터 아이템 클릭 시, 리스너를 호출하고 선택 상태를 업데이트한다
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    FilterItem item = getItem(position);
                    listener.onFilterClick(item, position);
                    setSelectedPosition(position);
                }
            });
        }


        //필터 데이터를 뷰에 바인딩하고 선택 상태에 따라 스타일을 적용한다.
        public void bind(FilterItem item, boolean isSelected) {
            textCategoryName.setText(item.getName());

            // 선택 상태에 따른 스타일 변경
            itemView.setSelected(isSelected);

            // 텍스트 색상 변경
            if (isSelected) {
                textCategoryName.setTextColor(Color.WHITE);
            } else {
                textCategoryName.setTextColor(itemView.getContext().getColor(android.R.color.black));
            }

            // 카테고리 색상 표시 (모두가 아닌 경우만)
            if (item.getColor() != null && !item.getColor().isEmpty()) {
                viewCategoryColor.setVisibility(View.VISIBLE);
                try {
                    int color = Color.parseColor(item.getColor());
                    viewCategoryColor.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(color));
                } catch (Exception e) {
                    viewCategoryColor.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(Color.GRAY));
                }
            } else {
                viewCategoryColor.setVisibility(View.GONE);
            }
        }
    }

    // 필터 아이템 데이터 클래스
    public static class FilterItem {
        private final String name; // 필터 이름 (예: "업무", "모두")
        private final String color; // 카테고리 색상
        private final Integer categoryId; // null이면 "모두"

        public FilterItem(String name, String color, Integer categoryId) {
            this.name = name;
            this.color = color;
            this.categoryId = categoryId;
        }

        public String getName() { return name; }
        public String getColor() { return color; }
        public Integer getCategoryId() { return categoryId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterItem that = (FilterItem) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(color, that.color) &&
                    Objects.equals(categoryId, that.categoryId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, color, categoryId);
        }
    }

    private static final DiffUtil.ItemCallback<FilterItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FilterItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull FilterItem oldItem, @NonNull FilterItem newItem) {
                    return Objects.equals(oldItem.getCategoryId(), newItem.getCategoryId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull FilterItem oldItem, @NonNull FilterItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
}