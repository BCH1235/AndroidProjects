package com.am.mytodolistapp.ui.collaboration;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.am.mytodolistapp.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

// 협업 프로젝트에 새로운 할 일을 추가하기 위한 UI를 제공하는 DialogFragment
// 사용자는 할 일의 제목, 내용, 마감 기한을 입력할 수 있다.
public class AddProjectTaskDialogFragment extends DialogFragment {

    private EditText editTaskTitle;
    private EditText editTaskContent;
    private CheckBox checkBoxSetDueDate;
    private TextView textSelectedDate;
    private Button buttonSelectDate;

    private OnTaskAddedListener listener;

    private Calendar selectedDueDate = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN);


    //할 일이 성공적으로 추가되었을 때 이벤트를 전달하기 위한 리스너 인터페이스
    public interface OnTaskAddedListener {
        void onTaskAdded(String title, String content, Long dueDate);
    }

    public void setOnTaskAddedListener(OnTaskAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_project_task, null);

        initViews(view);
        setupDatePicker();
        setupClickListeners(view);

        builder.setView(view);
        return builder.create();
    }

    private void initViews(View view) {
        editTaskTitle = view.findViewById(R.id.edit_task_title);
        editTaskContent = view.findViewById(R.id.edit_task_content);
        checkBoxSetDueDate = view.findViewById(R.id.checkbox_set_due_date);
        textSelectedDate = view.findViewById(R.id.text_selected_date);
        buttonSelectDate = view.findViewById(R.id.button_select_date);
    }


    //마감 기한 설정 관련 UI(체크박스, 날짜 선택 버튼)를 설정
    private void setupDatePicker() {
        textSelectedDate.setVisibility(View.GONE);
        buttonSelectDate.setVisibility(View.GONE);

        checkBoxSetDueDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textSelectedDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            buttonSelectDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            // 체크박스가 선택되고 아직 날짜가 설정되지 않았다면, 오늘 날짜를 기본값으로 설정
            if (isChecked && selectedDueDate == null) {
                selectedDueDate = Calendar.getInstance();
                updateDateDisplay();
            } else if (!isChecked) {
                selectedDueDate = null;
            }
        });

        buttonSelectDate.setOnClickListener(v -> showDatePickerDialog());
    }


    //'취소' 및 '추가' 버튼의 클릭 이벤트를 설정
    private void setupClickListeners(View view) {
        Button buttonCancel = view.findViewById(R.id.button_cancel);
        Button buttonAdd = view.findViewById(R.id.button_add);

        buttonCancel.setOnClickListener(v -> dismiss());
        buttonAdd.setOnClickListener(v -> addTask());
    }

    //DatePickerDialog를 생성하고 표시
    private void showDatePickerDialog() {
        Calendar calendar = selectedDueDate != null ? selectedDueDate : Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // 사용자가 날짜를 선택하면 selectedDueDate를 업데이트하고 화면에 표시
                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDueDate.set(Calendar.MILLISECOND, 0);
                    updateDateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    //선택된 날짜를 TextView에 포맷에 맞게 표시
    private void updateDateDisplay() {
        if (selectedDueDate != null) {
            textSelectedDate.setText("기한: " + dateFormat.format(selectedDueDate.getTime()));
        }
    }


    //사용자가 입력한 정보로 새 할 일을 추가하는 로직을 실행
    private void addTask() {
        String title = editTaskTitle.getText().toString().trim();
        String content = editTaskContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "할 일 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Long dueDate = null;
        if (checkBoxSetDueDate.isChecked() && selectedDueDate != null) {
            dueDate = selectedDueDate.getTimeInMillis();
        }

        if (listener != null) {
            listener.onTaskAdded(title, content, dueDate);
        }
        dismiss();
    }
}