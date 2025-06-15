package com.am.mytodolistapp.ui.calendar;

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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarDay> calendarDays; // CalendarDay 클래스 사용
    private LocalDate selectedDate;
    private final LocalDate today;
    private final OnDateClickListener onDateClickListener;
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
        return calendarDays != null ? calendarDays.size() : 0;
    }

    public void updateCalendar(List<CalendarDay> newCalendarDays) {
        this.calendarDays = newCalendarDays;
        notifyDataSetChanged();
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    public void setToday(LocalDate today) {
        // today는 final이므로 이 메서드를 제거하거나 필요시 새로운 방식으로 처리
        notifyDataSetChanged();
    }

    public void setCompletionRates(Map<LocalDate, Float> rates) {
        this.completionRates = rates != null ? rates : new HashMap<>();
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
                if (position != RecyclerView.NO_POSITION && onDateClickListener != null && calendarDays != null) {
                    CalendarDay calendarDay = calendarDays.get(position);
                    if (calendarDay.isCurrentMonth()) { // CalendarDay의 메서드 사용
                        onDateClickListener.onDateClick(calendarDay.getDate());
                    }
                }
            });
        }

        public void bind(CalendarDay calendarDay, float completionRate) {
            LocalDate date = calendarDay.getDate();
            textDay.setText(String.valueOf(date.getDayOfMonth()));

            resetStyles();

            // 현재 월이 아닌 날짜는 흐리게 표시
            if (!calendarDay.isCurrentMonth()) {
                textDay.setAlpha(0.3f);
                progressBarCompletion.setVisibility(View.GONE);
                return;
            } else {
                textDay.setAlpha(1.0f);
                progressBarCompletion.setVisibility(View.VISIBLE);
            }

            // 완료율 진행률 설정
            progressBarCompletion.setProgress((int) (completionRate * 100));

            // 완료율
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

            // 오늘 날짜 스타일 적용
            if (date.equals(today)) {
                setTodayStyle();
            }

            // 선택된 날짜 스타일 적용
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