package com.am.mytodolistapp.ui;

import androidx.annotation.NonNull;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.Collection;
import java.util.HashSet;

// MaterialCalendarView 의 특정 날짜 아래에 지정된 색상의 점을 추가하는 클래스
public class EventDecorator implements DayViewDecorator {

    private final int color; // 점 색상
    private final HashSet<CalendarDay> dates; // 점을 표시할 날짜 집합

    //점 색상과 점을 찍을 날짜 목록을 받아 초기화
    public EventDecorator(int color, @NonNull Collection<CalendarDay> dates) {
        this.color = color;
        this.dates = new HashSet<>(dates); // 중복 방지 및 빠른 조회를 위해 HashSet 사용
    }

    // 이 점을 특정 날짜(day)에 적용해야 하는지 결정
    @Override
    public boolean shouldDecorate(CalendarDay day) {
        // 전달받은 날짜가 점을 찍어야 하는 날짜 목록에 포함되어 있는지 확인
        return dates.contains(day);
    }

    // 날짜 뷰에 실제 점 적용
    @Override
    public void decorate(DayViewFacade view) {
        // 지정된 색상으로 점을 추가하여 날짜 아래에 표시
        view.addSpan(new DotSpan(10, color)); // DotSpan(반지름_크기, 색상)
    }
}