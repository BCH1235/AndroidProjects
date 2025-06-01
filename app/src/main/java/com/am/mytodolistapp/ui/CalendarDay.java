package com.am.mytodolistapp.ui;

import java.time.LocalDate;

public class CalendarDay {
    private LocalDate date;
    private boolean isCurrentMonth;
    private boolean hasEvents;

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