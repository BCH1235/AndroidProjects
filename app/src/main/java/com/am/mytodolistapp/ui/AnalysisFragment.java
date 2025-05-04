package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import org.joda.time.LocalDate;

import java.util.HashSet;
import java.util.Set;

// 할 일 완료 현황을 캘린더와 함께 분석/표시하는 화면
public class AnalysisFragment extends Fragment {

    private MaterialCalendarView calendarView; // 캘린더 UI 컴포넌트
    private TextView textViewAnalysisDetails; // 선택된 날짜 분석 내용 표시
    private AnalysisViewModel analysisViewModel; // 분석 데이터 및 로직 처리 ViewModel

    private final Set<EventDecorator> currentDecorators = new HashSet<>(); // 현재 캘린더에 적용된 점들을 추적/관리

    // 화면의 레이아웃(XML)을 View 객체로 생성
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    // View 생성 후 초기화 작업 수행 (ViewModel 연결, UI 요소 찾기, 리스너 설정, 데이터 관찰 시작)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 인스턴스 가져오기
        analysisViewModel = new ViewModelProvider(this).get(AnalysisViewModel.class);

        // UI 요소들 찾기
        calendarView = view.findViewById(R.id.calendar_view_analysis);
        textViewAnalysisDetails = view.findViewById(R.id.text_view_analysis_details);

        setupCalendar();// 캘린더 초기화 및 이벤트 리스너 설정

        observeAnalysisResult();// 일별 분석 결과 데이터 관찰 시작

        observeCompletedDays();// 월별 완료된 날짜 데이터 관찰 시작 (캘린더 점 표시용)
    }


    // 캘린더 초기 설정 (기본 날짜 선택, ViewModel 에 초기 날짜/월 전달, 리스너 설정)
    private void setupCalendar() {

        CalendarDay today = CalendarDay.today();

        calendarView.setSelectedDate(today);// 오늘 날짜 기본 선택

        // ViewModel 에도 초기 날짜(오늘) 설정 (일별 상세 분석용)
        analysisViewModel.setSelectedDate(LocalDate.now());

        // ViewModel 에 초기 월(오늘이 속한 월) 설정 (월별 점 표시용)
        analysisViewModel.setCurrentMonth(new LocalDate(today.getYear(), today.getMonth(), today.getDay()));

        // 날짜 선택 시 처리: ViewModel 에 선택된 날짜 업데이트
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (selected) {
                    LocalDate selectedDate = new LocalDate(date.getYear(), date.getMonth(), date.getDay());
                    analysisViewModel.setSelectedDate(selectedDate);// ViewModel 에 날짜 전달
                } else {
                    analysisViewModel.setSelectedDate(null);// 선택 해제 시 ViewModel 에 null 전달
                    textViewAnalysisDetails.setText("날짜를 선택하면 분석 내용이 표시됩니다.");// 기본 안내 문구 표시
                    textViewAnalysisDetails.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary_default));
                }
            }
        });

        // 월 변경 시 처리: ViewModel 에 변경된 월 업데이트
        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                // 월이 변경되면 ViewModel 에 알림
                LocalDate currentMonth = new LocalDate(date.getYear(), date.getMonth(), date.getDay());
                analysisViewModel.setCurrentMonth(currentMonth); // ViewModel 에 월 전달
            }
        });

    }

    // ViewModel 의 일별 분석 결과(LiveData)를 관찰하여 UI 업데이트
    private void observeAnalysisResult() {
        analysisViewModel.getAnalysisResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.completedCount > 0) {
                // 결과가 있으면 상세 내용 표시 함수 호출
                updateAnalysisDetails(result);
            } else {
                // 결과가 없으면 기본 메시지 표시
                textViewAnalysisDetails.setText("선택된 날짜에 완료된 할 일이 없습니다.");
                textViewAnalysisDetails.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary_default));
            }
        });
    }

    // ViewModel 의 월별 완료된 날짜 목록(LiveData)을 관찰하여 캘린더에 점 표시 업데이트
    private void observeCompletedDays() {
        analysisViewModel.getCompletedDays().observe(getViewLifecycleOwner(), completedDaysSet -> {
            // LiveData 가 업데이트될 때마다 실행됨
            //기존 점들 제거
            for (EventDecorator decorator : currentDecorators) {
                calendarView.removeDecorator(decorator);
            }
            currentDecorators.clear(); // 저장된 점 목록 비우기

            // 새로운 완료된 날짜들에 점 추가
            if (completedDaysSet != null && !completedDaysSet.isEmpty()) {
                // 새 EventDecorator 생성 (예: 파란색 점)
                EventDecorator completedDayDecorator = new EventDecorator(
                        ContextCompat.getColor(requireContext(), R.color.time_same), // 점 색상 설정
                        completedDaysSet // 완료된 날짜 목록
                );


                calendarView.addDecorator(completedDayDecorator); // 캘린더에 점 추가

                currentDecorators.add(completedDayDecorator);// 추가된 점 추적
            }

        });
    }

    // 전달받은 분석 결과를 텍스트로 포맷하여 TextView 에 표시하고 색상 적용
    private void updateAnalysisDetails(AnalysisViewModel.AnalysisResult result) {
        StringBuilder details = new StringBuilder();
        // 내용 구성 (완료 개수, 예상/실제 시간)
        details.append("완료된 할 일: ").append(result.completedCount).append("개\n\n");
        details.append("총 예상 시간: ").append(result.totalEstimatedTime).append("분\n");
        details.append("총 실제 시간: ").append(result.totalActualTime).append("분");

        int colorResId; // 텍스트 색상을 결정할 변수

        // 시간 차이 계산 및 표시 (예상 시간이 있을 때만)
        if (result.totalEstimatedTime > 0) {
            int timeDiff = result.totalActualTime - result.totalEstimatedTime;
            details.append("\n\n시간 차이: "); // 줄바꿈 추가
            if (timeDiff > 0) {
                details.append("+").append(timeDiff).append("분 (초과)");
                colorResId = R.color.time_slower; // 빨간색
            } else if (timeDiff < 0) {
                details.append(timeDiff).append("분 (단축)");
                colorResId = R.color.time_faster; // 초록색
            } else {
                details.append("0분 (일치)");
                colorResId = R.color.time_same; // 파란색
            }
        } else { // 예상 시간이 없는 경우
            details.append("\n\n시간 차이: 비교 불가"); // 줄바꿈 추가
            colorResId = R.color.text_secondary_default; // 기본색
        }

        // TextView 에 텍스트 및 색상 적용
        textViewAnalysisDetails.setText(details.toString());
        textViewAnalysisDetails.setTextColor(ContextCompat.getColor(requireContext(), colorResId));
    }


}