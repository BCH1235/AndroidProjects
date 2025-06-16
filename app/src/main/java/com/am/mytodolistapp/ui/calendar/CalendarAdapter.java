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


// 캘린더의 날짜 그리드를 표시하기 위한 RecyclerView 어댑터.
// 각 날짜의 UI(숫자, 선택 상태, 오늘 표시, 완료율)를 관리
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarDay> calendarDays; // 캘린더에 표시될 날짜 데이터 목록
    private LocalDate selectedDate; // 사용자가 선택한 날짜
    private final LocalDate today; // 오늘 날짜
    private final OnDateClickListener onDateClickListener; // 날짜 클릭 이벤트를 처리할 리스너
    private Map<LocalDate, Float> completionRates;  // 날짜별 할 일 완료율 맵


    // 날짜 클릭 이벤트를 위한 인터페이스.
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
    // 캘린더 데이터를 새로운 목록으로 업데이트하고 UI를 갱신
    public void updateCalendar(List<CalendarDay> newCalendarDays) {
        this.calendarDays = newCalendarDays;
        notifyDataSetChanged();
    }

    //사용자가 선택한 날짜를 설정하고 UI를 갱신
    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    public void setToday(LocalDate today) {
        // today는 final이므로 이 메서드를 제거하거나 필요시 새로운 방식으로 처리
        notifyDataSetChanged();
    }

    // 날짜별 완료율 데이터를 설정하고 UI를 갱신
    public void setCompletionRates(Map<LocalDate, Float> rates) {
        this.completionRates = rates != null ? rates : new HashMap<>();
        notifyDataSetChanged();
    }


    //각 날짜 아이템의 뷰를 관리하는 ViewHolder 클래스
    class CalendarViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDay;
        private final FrameLayout containerDay;
        private final ProgressBar progressBarCompletion;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.text_day);
            containerDay = itemView.findViewById(R.id.container_day);
            progressBarCompletion = itemView.findViewById(R.id.progress_bar_completion);

            // 아이템 클릭 시 리스너 호출
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDateClickListener != null && calendarDays != null) {
                    CalendarDay calendarDay = calendarDays.get(position);
                    // 현재 월에 해당하는 날짜만 클릭 가능하도록 처리
                    if (calendarDay.isCurrentMonth()) { // CalendarDay의 메서드 사용
                        onDateClickListener.onDateClick(calendarDay.getDate());
                    }
                }
            });
        }


        // 데이터를 뷰에 바인딩(연결)하고 UI 스타일을 적용
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

            // 완료율에 따라 프로그레스바의 진행률과 색상을 설정
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
        }// 뷰의 스타일을 기본 상태로 초기화

        private void setTodayStyle() {
            GradientDrawable todayBackground = new GradientDrawable();
            todayBackground.setShape(GradientDrawable.OVAL);
            todayBackground.setStroke(2, ContextCompat.getColor(itemView.getContext(), R.color.calendar_today));
            todayBackground.setColor(Color.TRANSPARENT);
            containerDay.setBackground(todayBackground);
            textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.teal_700));
        }// 오늘 날짜에 해당하는 테두리를 적용

        private void setSelectedStyle() {
            GradientDrawable selectedBackground = new GradientDrawable();
            selectedBackground.setShape(GradientDrawable.OVAL);
            selectedBackground.setColor(ContextCompat.getColor(itemView.getContext(), R.color.calendar_selected));
            containerDay.setBackground(selectedBackground);
            textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
        } // 선택된 날짜에 해당하는 배경색을 적용
    }
}