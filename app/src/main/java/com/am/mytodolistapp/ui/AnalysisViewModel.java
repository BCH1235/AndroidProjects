package com.am.mytodolistapp.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.data.TodoRepository;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


// AnalysisFragment 에 필요한 데이터 제공 및 비즈니스 로직 처리
public class AnalysisViewModel extends AndroidViewModel {

    private TodoRepository mRepository; // 데이터 저장소 접근 객체

    // UI 에서 선택/변경된 날짜 및 월 상태 저장
    private MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>(); // 현재 선택된 날짜
    private MutableLiveData<LocalDate> currentMonth = new MutableLiveData<>(); // 현재 캘린더가 보여주는 월

    // Repository 에서 가져온 원본 데이터 (선택된 날짜/월 기반)
    private LiveData<List<TodoItem>> completedTodosForSelectedDate; // 선택된 날짜에 완료된 할 일 목록

    private LiveData<List<TodoItem>> completedTodosForCurrentMonth; // 현재 월에 완료된 할 일 목록

    // 가공된 최종 결과 데이터
    private MediatorLiveData<AnalysisResult> analysisResult = new MediatorLiveData<>(); // 일별 분석 결과

    private MediatorLiveData<Set<CalendarDay>> completedDays = new MediatorLiveData<>(); // 월별 완료된 날짜 Set (캘린더 점 표시용)


    // ViewModel 생성 시 초기화 로직 (Repository 연결, 데이터 흐름 정의)
    public AnalysisViewModel(@NonNull Application application) {
        super(application);
        mRepository = new TodoRepository(application);

        // 선택된 날짜(selectedDate)가 변경되면, 해당 날짜의 완료된 할 일 목록(completedTodosForSelectedDate)을 Repository 에서 가져옴
        completedTodosForSelectedDate = androidx.lifecycle.Transformations.switchMap(selectedDate, date -> {
            if (date == null) {
                return new MutableLiveData<>(); // 날짜 없으면 빈 LiveData 반환
            }
            // 날짜 범위(시작~끝 타임스탬프) 계산
            DateTime startOfDay = date.toDateTimeAtStartOfDay();
            DateTime endOfDay = startOfDay.plusDays(1);
            long startTime = startOfDay.getMillis();
            long endTime = endOfDay.getMillis();
            // Repository 에 데이터 요청
            return mRepository.getCompletedTodosBetween(startTime, endTime);
        });

        // 선택된 날짜의 완료된 할 일 목록(completedTodosForSelectedDate)이 변경(갱신)되면, 일별 분석 결과(analysisResult) 계산
        analysisResult.addSource(completedTodosForSelectedDate, todos -> {
            if (todos != null) calculateAnalysis(todos);
            else analysisResult.setValue(null); // 데이터 없으면 null 설정
        });

        // 현재 월(currentMonth)이 변경되면, 해당 월의 완료된 할 일 목록(completedTodosForCurrentMonth)을 Repository 에서 가져옴
        completedTodosForCurrentMonth = Transformations.switchMap(currentMonth, month -> {
            if (month == null) return new MutableLiveData<>();
            // 월 범위(시작~끝 타임스탬프) 계산
            DateTime startOfMonth = month.toDateTimeAtStartOfDay().withDayOfMonth(1);
            DateTime startOfNextMonth = startOfMonth.plusMonths(1);
            // Repository 에 데이터 요청
            return mRepository.getCompletedTodosBetween(startOfMonth.getMillis(), startOfNextMonth.getMillis());
        });

        // 현재 월의 완료된 할 일 목록(completedTodosForCurrentMonth)이 변경(갱신)되면, 완료된 날짜 Set(completedDays) 계산
        completedDays.addSource(completedTodosForCurrentMonth, todos -> {
            if (todos != null) {
                calculateCompletedDays(todos); // 완료된 날짜 계산 함수 호출
            }
        });
    }


    public void setSelectedDate(LocalDate date) {
        selectedDate.setValue(date);
    }// Fragment 에서 선택된 날짜를 ViewModel 에 알림

    public void setCurrentMonth(LocalDate month) {
        currentMonth.setValue(month);
    }// Fragment 에서 변경된 월을 ViewModel 에 알림


    public LiveData<AnalysisResult> getAnalysisResult() {
        return analysisResult;
    }// Fragment 가 관찰할 일별 분석 결과 LiveData 제공

    public LiveData<Set<CalendarDay>> getCompletedDays() {
        return completedDays;
    }// Fragment 가 관찰할 월별 완료된 날짜 Set LiveData 제공 (캘린더 점 표시용)


    private void calculateAnalysis(List<TodoItem> completedTodos) {
        int totalEstimated = 0;
        int totalActual = 0;
        int completedCount = completedTodos.size();
        // 시간 합계 계산
        for (TodoItem item : completedTodos) {
            totalEstimated += item.getEstimatedTimeMinutes();
            totalActual += item.getActualTimeMinutes();
        }

        // 계산된 결과를 AnalysisResult 객체에 담아 LiveData 에 설정
        analysisResult.setValue(new AnalysisResult(completedCount, totalEstimated, totalActual));
    }// 전달받은 할 일 목록으로 일별 분석 결과 계산



    // 전달받은 월별 할 일 목록으로 완료된 날짜 Set 계산
    private void calculateCompletedDays(List<TodoItem> monthlyCompletedTodos) { // <<--- 완료된 날짜 계산 함수 추가
        Set<CalendarDay> daysWithCompletion = new HashSet<>();// 완료된 날짜들을 CalendarDay 형태로 변환하여 Set 에 추가
        for (TodoItem item : monthlyCompletedTodos) {
            if (item.getCompletionTimestamp() > 0) {
                // 완료 타임스탬프를 LocalDate 로 변환
                LocalDate completionDate = new DateTime(item.getCompletionTimestamp()).toLocalDate();
                CalendarDay calendarDay = CalendarDay.from(completionDate.getYear(),
                        completionDate.getMonthOfYear(),
                        completionDate.getDayOfMonth());
                daysWithCompletion.add(calendarDay);
            }
        }
        completedDays.setValue(daysWithCompletion);// 계산된 Set 을 LiveData 에 설정하여 UI 에 알림
    }

    // 일별 분석 결과를 담는 데이터 클래스
    public static class AnalysisResult {
        public final int completedCount; // 완료된 할 일 개수
        public final int totalEstimatedTime; // 총 예상 시간 합계
        public final int totalActualTime; // 총 실제 시간 합계


        public AnalysisResult(int completedCount, int totalEstimatedTime, int totalActualTime) {
            this.completedCount = completedCount;
            this.totalEstimatedTime = totalEstimatedTime;
            this.totalActualTime = totalActualTime;
        }
    }
}