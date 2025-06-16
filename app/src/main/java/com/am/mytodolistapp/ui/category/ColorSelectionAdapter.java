package com.am.mytodolistapp.ui.category;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;

// 카테고리 추가/수정 다이얼로그에서 색상을 선택하는 UI를 위한 RecyclerView 어댑터
// 원형의 색상 옵션을 표시하고, 사용자가 선택한 색상을 관리한다.
public class ColorSelectionAdapter extends RecyclerView.Adapter<ColorSelectionAdapter.ColorViewHolder> {
    private final String[] colors; // 표시할 색상 배열
    private final OnColorSelectedListener listener; // 색상 선택 이벤트를 전달할 리스너
    private String selectedColor; // 현재 선택된 색상

    // 색상 선택 이벤트를 위한 인터페이스.
    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }



    public ColorSelectionAdapter(String[] colors, OnColorSelectedListener listener) {
        this.colors = colors;
        this.listener = listener;
    }


    //외부에서 선택된 색상을 설정하고 UI를 갱신합니다.
    public void setSelectedColor(String color) {
        this.selectedColor = color;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.color_selector_item, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        String color = colors[position];
        holder.bind(color, color.equals(selectedColor));
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    class ColorViewHolder extends RecyclerView.ViewHolder {
        private final View viewColor;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColor = itemView.findViewById(R.id.view_color);

            // 아이템 클릭 시, 선택된 색상을 업데이트하고 리스너를 호출
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    selectedColor = colors[position];
                    listener.onColorSelected(selectedColor);
                    notifyDataSetChanged();
                }
            });
        }


        //색상 데이터를 뷰에 바인딩하고 선택 상태에 따라 스타일을 적용한다
        public void bind(String color, boolean isSelected) {
            try {
                int colorInt = Color.parseColor(color);

                // 원형 Drawable
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                drawable.setColor(colorInt);

                if (isSelected) {
                    //선택된 색상: 검은색 테두리 추가
                    drawable.setStroke(8, Color.BLACK); // 8px 검은색 테두리
                } else {
                    //선택되지 않은 색상: 테두리 없음
                    drawable.setStroke(0, Color.TRANSPARENT);
                }

                viewColor.setBackground(drawable);

            } catch (Exception e) {
                // 기본 색상 사용
                GradientDrawable defaultDrawable = new GradientDrawable();
                defaultDrawable.setShape(GradientDrawable.OVAL);
                defaultDrawable.setColor(Color.parseColor("#FF4444"));
                viewColor.setBackground(defaultDrawable);
            }
        }
    }
}