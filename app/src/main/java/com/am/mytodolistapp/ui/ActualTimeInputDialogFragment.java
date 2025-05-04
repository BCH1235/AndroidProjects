package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;

// '실제 소요 시간'을 입력받는 다이얼로그 창
public class ActualTimeInputDialogFragment extends DialogFragment {



    private NumberPicker numberPickerHourActual;   // 시간 선택 UI
    private NumberPicker numberPickerMinuteActual; // 분 선택 UI
    private Button buttonCancelActualTime; // 취소 버튼
    private Button buttonConfirmActualTime; // 확인 버튼
    private TaskListViewModel taskListViewModel; // 할 일 목록 ViewModel

    // 완료 처리할 TodoItem 의 ID 를 전달받기 위한 키
    private static final String ARG_TODO_ID = "todo_id";
    private int todoId;// 전달받은 할 일 ID 저장 변수

    // 다이얼로그 인스턴스 생성 및 할 일 ID 전달
    public static ActualTimeInputDialogFragment newInstance(int todoId) {
        ActualTimeInputDialogFragment fragment = new ActualTimeInputDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TODO_ID, todoId);
        fragment.setArguments(args);
        return fragment;
    }

    // 프래그먼트 생성 시 초기 설정 (ViewModel 연결, ID 가져오기)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);// ViewModel 가져오기
        taskListViewModel = new ViewModelProvider(requireActivity()).get(TaskListViewModel.class);

        // 전달받은 할 일 ID 확인
        if (getArguments() != null) {
            todoId = getArguments().getInt(ARG_TODO_ID);
        } else {
            dismiss(); // ID 없으면 닫기
        }
    }

    // 다이얼로그의 레이아웃(XML)을 화면에 표시할 View 객체로 생성
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_input_actual_time, container, false);
    }

    // View 생성 후 UI 요소들 초기화 및 이벤트 리스너 설정
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소들 찾기
        numberPickerHourActual = view.findViewById(R.id.number_picker_hour_actual);     // <<--- NumberPicker 찾기
        numberPickerMinuteActual = view.findViewById(R.id.number_picker_minute_actual); // <<--- NumberPicker 찾기
        buttonCancelActualTime = view.findViewById(R.id.button_cancel_actual_time);
        buttonConfirmActualTime = view.findViewById(R.id.button_confirm_actual_time);

        // NumberPicker 범위 및 초기값 설정
        numberPickerHourActual.setMinValue(0);
        numberPickerHourActual.setMaxValue(23);
        numberPickerMinuteActual.setMinValue(0);
        numberPickerMinuteActual.setMaxValue(59);
        // 초기값은 0으로 설정
        numberPickerHourActual.setValue(0);
        numberPickerMinuteActual.setValue(0);

        // 취소 버튼 클릭 시: 다이얼로그 닫기
        buttonCancelActualTime.setOnClickListener(v -> dismiss());

        // 확인 버튼 클릭 시: 입력값 처리 및 ViewModel 에 전달
        buttonConfirmActualTime.setOnClickListener(v -> {
            numberPickerHourActual.clearFocus();   // 시간 NumberPicker 포커스 제거
            numberPickerMinuteActual.clearFocus(); // 분 NumberPicker 포커스 제거
            // String actualTimeString = ... // 기존 코드 삭제

            // 선택된 시간/분으로 총 분 계산
            int hour = numberPickerHourActual.getValue();
            int minute = numberPickerMinuteActual.getValue();
            int actualMinutes = (hour * 60) + minute;


            // ViewModel 에 완료 처리 요청 (ID 와 실제 소요 시간 전달)
            taskListViewModel.markAsComplete(todoId, actualMinutes);
            dismiss();
        });


    }

    // 역할: 다이얼로그 크기 조절
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}