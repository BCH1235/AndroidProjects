package com.am.mytodolistapp.ui;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;

import java.text.SimpleDateFormat; // 날짜 포맷 관련
import java.util.Date;           // Date 관련
import java.util.Locale;         // 지역 설정 관련

// '할 일 목록'을 RecyclerView 에 표시하기 위한 어댑터
public class TaskListAdapter extends ListAdapter<TodoItem, TaskListAdapter.TaskViewHolder> {

    private final TaskListViewModel viewModel; // 상호작용을 위한 ViewModel 참조


    protected TaskListAdapter(TaskListViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    // RecyclerView 가 새 항목 뷰(ViewHolder)를 필요로 할 때 호출됨
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // list_item_todo.xml 레이아웃을 사용하여 뷰 생성
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.list_item_todo, parent, false);
        return new TaskViewHolder(itemView, viewModel);
    }

    // 특정 위치의 항목 뷰에 데이터를 표시해야 할 때 호출됨
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TodoItem currentTodo = getItem(position); // 해당 위치의 데이터 가져오기
        holder.bind(currentTodo); // ViewHolder 에 데이터 바인딩 요청
    }

    // ViewHolder 클래스
    // RecyclerView 의 각 항목에 대한 뷰 요소들을 보관하고 관리
    class TaskViewHolder extends RecyclerView.ViewHolder {
        // 항목 레이아웃 내의 UI 요소들
        private final CheckBox checkBoxCompleted;       // 완료 여부 체크박스
        private final TextView textViewTitle;           // 할 일 제목 텍스트뷰
        private final TextView textViewEstimatedTime;   // 예상 시간 텍스트뷰
        private final TextView textViewActualTime;      // 실제 시간 텍스트뷰
        private final TextView textViewCompletionTime;  // 완료 시각 텍스트뷰
        private final TaskListViewModel viewModel;      // 이벤트 처리를 위한 ViewModel 참조

        // ViewHolder 생성 시: UI 요소 찾아 멤버 변수에 할당 및 리스너 설정
        public TaskViewHolder(@NonNull View itemView, TaskListViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel; // ViewModel 참조 저장

            // UI 요소 찾기
            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewEstimatedTime = itemView.findViewById(R.id.text_view_estimated_time);
            textViewActualTime = itemView.findViewById(R.id.text_view_actual_time);
            textViewCompletionTime = itemView.findViewById(R.id.text_view_completion_time);

            // 체크박스 클릭 이벤트 처리
            checkBoxCompleted.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // 클릭된 항목의 위치 확인
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem clickedTodo = getItem(position); // 클릭된 항목 데이터 가져오기
                    boolean newState = !clickedTodo.isCompleted(); // 새로운 완료 상태 계산

                    if (newState) { // '완료' 상태로 변경 시
                        // 실제 시간 입력 다이얼로그 표시 요청
                        ActualTimeInputDialogFragment dialogFragment =
                                ActualTimeInputDialogFragment.newInstance(clickedTodo.getId());
                        // Activity 의 FragmentManager 를 통해 다이얼로그 표시
                        if (itemView.getContext() instanceof AppCompatActivity) {
                            AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                            dialogFragment.show(activity.getSupportFragmentManager(), "ActualTimeInputDialog");
                        } else {
                            Log.e("TaskListAdapter", "Cannot get FragmentManager from context");
                        }
                    } else { // '미완료' 상태로 변경 시 (체크 해제)
                        // 상태 변경 및 관련 시간 정보 초기화 후 즉시 업데이트 요청
                        clickedTodo.setCompleted(false);
                        clickedTodo.setActualTimeMinutes(0);
                        clickedTodo.setCompletionTimestamp(0);
                        viewModel.update(clickedTodo);
                    }
                }
            });

            // 항목 전체 클릭 이벤트 처리
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // 클릭된 항목의 위치 확인
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem clickedTodo = getItem(position); // 클릭된 항목 데이터 가져오기
                    // 수정 다이얼로그 표시 요청
                    EditTodoDialogFragment dialogFragment = EditTodoDialogFragment.newInstance(clickedTodo);
                    // Activity 의 FragmentManager 를 통해 다이얼로그 표시
                    if (itemView.getContext() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                        dialogFragment.show(activity.getSupportFragmentManager(), "EditTodoDialog");
                    } else {
                        Log.e("TaskListAdapter", "Cannot get FragmentManager from context");
                    }
                }
            });
        }

        // TodoItem 데이터를 받아서 ViewHolder 내부의 뷰들에 표시
        public void bind(TodoItem todoItem) {
            // 제목 및 체크 상태 설정
            textViewTitle.setText(todoItem.getTitle());
            checkBoxCompleted.setChecked(todoItem.isCompleted());

            // 예상 시간 표시 설정
            int estimatedMinutes = todoItem.getEstimatedTimeMinutes();
            if (estimatedMinutes > 0) {
                textViewEstimatedTime.setText("예상: " + formatMinutesToHoursAndMinutes(estimatedMinutes));
                textViewEstimatedTime.setVisibility(View.VISIBLE);
            } else {
                textViewEstimatedTime.setVisibility(View.GONE);
            }

            // 완료 상태에 따른 실제 시간 및 완료 시각 표시 설정
            if (todoItem.isCompleted()) { // 완료된 경우
                int actualMinutes = todoItem.getActualTimeMinutes();

                // 실제 시간 표시 및 색상 설정
                if (actualMinutes > 0) {
                    textViewActualTime.setText("실제: " + formatMinutesToHoursAndMinutes(actualMinutes));
                    // 예상 시간 대비 실제 시간 비교하여 색상 결정
                    int colorResId;
                    if (estimatedMinutes <= 0) { // 예상 시간 없으면 기본색
                        colorResId = R.color.text_secondary_default;
                    } else if (actualMinutes < estimatedMinutes) { // 단축 시
                        colorResId = R.color.time_faster;
                    } else if (actualMinutes > estimatedMinutes) { // 초과 시
                        colorResId = R.color.time_slower;
                    } else { // 일치 시
                        colorResId = R.color.time_same;
                    }
                    textViewActualTime.setTextColor(ContextCompat.getColor(itemView.getContext(), colorResId));
                    textViewActualTime.setVisibility(View.VISIBLE);
                } else {
                    textViewActualTime.setVisibility(View.GONE);
                }

                // 완료 시각 표시 설정
                long completionTimestamp = todoItem.getCompletionTimestamp();
                if (completionTimestamp > 0) {
                    textViewCompletionTime.setText("완료: " + formatTimestamp(completionTimestamp));
                    textViewCompletionTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary_default));
                    textViewCompletionTime.setVisibility(View.VISIBLE);
                } else {
                    textViewCompletionTime.setVisibility(View.GONE);
                }

            } else { // 미완료된 경우
                // 실제 시간 및 완료 시각 숨김
                textViewActualTime.setVisibility(View.GONE);
                textViewCompletionTime.setVisibility(View.GONE);
            }
        }

        // 타임스탬프를 "yyyy-MM-dd HH:mm" 형식의 문자열로 변환
        private String formatTimestamp(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        // 총 분을 "X시간 Y분" 형식의 문자열로 변환
        private String formatMinutesToHoursAndMinutes(int totalMinutes) {
            if (totalMinutes <= 0) return "0분";
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            StringBuilder sb = new StringBuilder();
            if (hours > 0) {
                sb.append(hours).append("시간");
                if (minutes > 0) sb.append(" ");
            }
            if (minutes > 0) {
                sb.append(minutes).append("분");
            }
            return sb.toString();
        }
    }

    // RecyclerView 가 리스트 업데이트 시 변경 사항을 효율적으로 계산하기 위한 콜백
    private static final DiffUtil.ItemCallback<TodoItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<TodoItem>() {
        // 두 아이템이 동일한 항목을 나타내는지 확인 (ID 비교)
        @Override
        public boolean areItemsTheSame(@NonNull TodoItem oldItem, @NonNull TodoItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        // 두 아이템의 내용(데이터)이 동일한지 확인
        @Override
        public boolean areContentsTheSame(@NonNull TodoItem oldItem, @NonNull TodoItem newItem) {
            // 제목, 완료상태, 예상/실제/완료 시간 비교
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.isCompleted() == newItem.isCompleted() &&
                    oldItem.getEstimatedTimeMinutes() == newItem.getEstimatedTimeMinutes() &&
                    oldItem.getActualTimeMinutes() == newItem.getActualTimeMinutes() &&
                    oldItem.getCompletionTimestamp() == newItem.getCompletionTimestamp();
        }
    };

}