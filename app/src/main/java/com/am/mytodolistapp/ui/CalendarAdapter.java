package com.am.mytodolistapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;

import java.time.LocalDate;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarFragment.CalendarDay> calendarDays;
    private LocalDate selectedDate;
    private LocalDate today;
    private OnDateClickListener onDateClickListener;

    public interface OnDateClickListener {
        void onDateClick(LocalDate date);
    }

    public CalendarAdapter(List<CalendarFragment.CalendarDay> calendarDays, OnDateClickListener listener) {
        this.calendarDays = calendarDays;
        this.onDateClickListener = listener;
        this.today = LocalDate.now();
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
        CalendarFragment.CalendarDay calendarDay = calendarDays.get(position);
        holder.bind(calendarDay);
    }

    @Override
    public int getItemCount() {
        return calendarDays.size();
    }

    public void updateCalendar(List<CalendarFragment.CalendarDay> newCalendarDays) {
        this.calendarDays = newCalendarDays;
        notifyDataSetChanged();
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    public void setToday(LocalDate today) {
        this.today = today;
        notifyDataSetChanged();
    }

    class CalendarViewHolder extends RecyclerView.ViewHolder {
        private TextView textDay;
        private View containerDay;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.text_day);
            containerDay = itemView.findViewById(R.id.container_day);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDateClickListener != null) {
                    CalendarFragment.CalendarDay calendarDay = calendarDays.get(position);
                    if (calendarDay.isCurrentMonth()) {
                        onDateClickListener.onDateClick(calendarDay.getDate());
                    }
                }
            });
        }

        public void bind(CalendarFragment.CalendarDay calendarDay) {
            LocalDate date = calendarDay.getDate();
            textDay.setText(String.valueOf(date.getDayOfMonth()));

            // 기본 스타일 초기화
            resetStyles();

            // 현재 월이 아닌 날짜는 흐리게 표시
            if (!calendarDay.isCurrentMonth()) {
                textDay.setAlpha(0.3f);
                return;
            } else {
                textDay.setAlpha(1.0f);
            }

            // 오늘 날짜 강조 (파란색 테두리)
            if (date.equals(today)) {
                setTodayStyle();
            }

            // 선택된 날짜 강조 (파란색 배경)
            if (date.equals(selectedDate)) {
                setSelectedStyle();
            }
        }

        private void resetStyles() {
            containerDay.setBackgroundResource(0);
            textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
        }

        private void setTodayStyle() {
            // 오늘 날짜는 파란색 테두리
            containerDay.setBackgroundResource(R.drawable.calendar_today_border);
            textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.teal_700));
        }

        private void setSelectedStyle() {
            // 선택된 날짜는 파란색 배경
            containerDay.setBackgroundResource(R.drawable.calendar_selected_background);
            textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
        }
    }
}