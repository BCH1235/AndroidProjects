package com.am.mytodolistapp.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarDay> calendarDays; // CalendarFragment.CalendarDay → CalendarDay로 변경
    private LocalDate selectedDate;
    private LocalDate today;
    private final OnDateClickListener onDateClickListener; // final 추가
    private Map<LocalDate, Float> completionRates;

    public interface OnDateClickListener {
        void onDateClick(LocalDate date);
    }

    public CalendarAdapter(List<CalendarDay> calendarDays, OnDateClickListener listener) {
        this.calendarDays = calendarDays;
        this.onDateClickListener = listener;
        this.today = LocalDate.now();
        this.completionRates = new HashMap<>();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay calendarDay = calendarDays.get(position);
        Float rate = completionRates.get(calendarDay.getDate());
        holder.bind(calendarDay, rate != null ? rate : 0f);
    }

    @Override
    public int getItemCount() {
        return calendarDays.size();
    }

    public void updateCalendar(List<CalendarDay> newCalendarDays) {
        this.calendarDays = newCalendarDays;
        notifyDataSetChanged();
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    public void setCompletionRates(Map<LocalDate, Float> rates) {
        this.completionRates = rates;
        notifyDataSetChanged();
    }

    class CalendarViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDay;
        private final FrameLayout containerDay;
        private final ProgressBar progressBarCompletion;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.text_day);
            containerDay = itemView.findViewById(R.id.container_day);
            progressBarCompletion = itemView.findViewById(R.id.progress_bar_completion);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDateClickListener != null) {
                    CalendarDay calendarDay = calendarDays.get(position);
                    if (calendarDay.isCurrentMonth()) {
                        onDateClickListener.onDateClick(calendarDay.getDate());
                    }
                }
            });
        }

        public void bind(CalendarDay calendarDay, float completionRate) {
            LocalDate date = calendarDay.getDate();
            textDay.setText(String.valueOf(date.getDayOfMonth()));

            resetStyles();

            if (!calendarDay.isCurrentMonth()) {
                textDay.setAlpha(0.3f);
                progressBarCompletion.setVisibility(View.GONE);
                return;
            } else {
                textDay.setAlpha(1.0f);
                progressBarCompletion.setVisibility(View.VISIBLE);
            }

            progressBarCompletion.setProgress((int) (completionRate * 100));

            // 완료율에 따라 ProgressBar의 색상 변경
            int progressColor;
            if (completionRate == 0) {
                progressColor = ContextCompat.getColor(itemView.getContext(), R.color.calendar_progress_empty);
            } else if (completionRate <= 0.33f) {
                progressColor = ContextCompat.getColor(itemView.getContext(), R.color.calendar_progress_low);
            } else if (completionRate <= 0.66f) {
                progressColor = ContextCompat.getColor(itemView.getContext(), R.color.calendar_progress_medium);
            } else {
                progressColor = ContextCompat.getColor(itemView.getContext(), R.color.calendar_progress_high);
            }
            progressBarCompletion.setProgressTintList(ColorStateList.valueOf(progressColor));

            if (date.equals(today)) {
                setTodayStyle();
            }

            if (date.equals(selectedDate)) {
                setSelectedStyle();
            }
        }

        private void resetStyles() {
            GradientDrawable defaultBackground = new GradientDrawable();
            defaultBackground.setShape(GradientDrawable.OVAL);
            defaultBackground.setColor(Color.TRANSPARENT);
            containerDay.setBackground(defaultBackground);
            textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
        }

        private void setTodayStyle() {
            GradientDrawable todayBackground = new GradientDrawable();
            todayBackground.setShape(GradientDrawable.OVAL);
            todayBackground.setStroke(2, ContextCompat.getColor(itemView.getContext(), R.color.calendar_today));
            todayBackground.setColor(Color.TRANSPARENT);
            containerDay.setBackground(todayBackground);
            textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.teal_700));
        }

        private void setSelectedStyle() {
            GradientDrawable selectedBackground = new GradientDrawable();
            selectedBackground.setShape(GradientDrawable.OVAL);
            selectedBackground.setColor(ContextCompat.getColor(itemView.getContext(), R.color.calendar_selected));
            containerDay.setBackground(selectedBackground);
            textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
        }
    }
}