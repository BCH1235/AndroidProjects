package com.am.mytodolistapp.ui.calendar;

import java.time.LocalDate;

// 캘린더의 각 날짜에 대한 정보를 담는 데이터 클래스
public class CalendarDay {
    private LocalDate date; // 해당 날짜
    private boolean isCurrentMonth; // 현재 캘린더에 표시된 월에 속하는지 여부
    private boolean hasEvents; // 해당 날짜에 할 일이 있는지 여부

    public CalendarDay(LocalDate date, boolean isCurrentMonth) {
        this.date = date;
        this.isCurrentMonth = isCurrentMonth;
        this.hasEvents = false;
    }

    public CalendarDay(LocalDate date, boolean isCurrentMonth, boolean hasEvents) {
        this.date = date;
        this.isCurrentMonth = isCurrentMonth;
        this.hasEvents = hasEvents;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isCurrentMonth() {
        return isCurrentMonth;
    }

    public void setCurrentMonth(boolean currentMonth) {
        isCurrentMonth = currentMonth;
    }

    public boolean hasEvents() {
        return hasEvents;
    }

    public void setHasEvents(boolean hasEvents) {
        this.hasEvents = hasEvents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarDay that = (CalendarDay) o;
        return isCurrentMonth == that.isCurrentMonth &&
                hasEvents == that.hasEvents &&
                date.equals(that.date);
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }
}