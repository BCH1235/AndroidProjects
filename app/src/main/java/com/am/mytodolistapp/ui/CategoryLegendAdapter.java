package com.am.mytodolistapp.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;

import java.util.List;

public class CategoryLegendAdapter extends RecyclerView.Adapter<CategoryLegendAdapter.LegendViewHolder> {

    private List<StatisticsViewModel.CategoryStatData> legendData;

    public CategoryLegendAdapter(List<StatisticsViewModel.CategoryStatData> legendData) {
        this.legendData = legendData;
    }

    public void updateData(List<StatisticsViewModel.CategoryStatData> newData) {
        this.legendData = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_legend, parent, false);
        return new LegendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LegendViewHolder holder, int position) {
        StatisticsViewModel.CategoryStatData item = legendData.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return legendData != null ? legendData.size() : 0;
    }

    static class LegendViewHolder extends RecyclerView.ViewHolder {
        private final View colorIndicator;
        private final TextView categoryName;
        private final TextView taskCount;

        public LegendViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
            categoryName = itemView.findViewById(R.id.category_name);
            taskCount = itemView.findViewById(R.id.task_count);
        }

        public void bind(StatisticsViewModel.CategoryStatData data) {
            // 색상 인디케이터 설정
            GradientDrawable colorDrawable = new GradientDrawable();
            colorDrawable.setShape(GradientDrawable.OVAL);
            try {
                colorDrawable.setColor(Color.parseColor(data.getColor()));
            } catch (Exception e) {
                colorDrawable.setColor(Color.GRAY);
            }
            colorIndicator.setBackground(colorDrawable);

            // 텍스트 설정
            categoryName.setText(data.getCategoryName());
            taskCount.setText(String.valueOf(data.getCount()));
        }
    }
}