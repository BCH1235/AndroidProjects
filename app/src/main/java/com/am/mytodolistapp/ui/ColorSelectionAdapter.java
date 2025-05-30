package com.am.mytodolistapp.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;

public class ColorSelectionAdapter extends RecyclerView.Adapter<ColorSelectionAdapter.ColorViewHolder> {
    private final String[] colors;
    private final OnColorSelectedListener listener;
    private String selectedColor;

    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }

    public ColorSelectionAdapter(String[] colors, OnColorSelectedListener listener) {
        this.colors = colors;
        this.listener = listener;
    }

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

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    selectedColor = colors[position];
                    listener.onColorSelected(selectedColor);
                    notifyDataSetChanged();
                }
            });
        }

        public void bind(String color, boolean isSelected) {
            try {
                int colorInt = Color.parseColor(color);


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