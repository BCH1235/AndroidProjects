package com.am.mytodolistapp.ui;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;

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
        private final TaskListViewModel viewModel;      // 이벤트 처리를 위한 ViewModel 참조

        // ViewHolder 생성 시: UI 요소 찾아 멤버 변수에 할당 및 리스너 설정
        public TaskViewHolder(@NonNull View itemView, TaskListViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel; // ViewModel 참조 저장

            // UI 요소 찾기
            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textViewTitle = itemView.findViewById(R.id.text_view_title);

            // 체크박스 클릭 이벤트 처리
            checkBoxCompleted.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // 클릭된 항목의 위치 확인
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem clickedTodo = getItem(position); // 클릭된 항목 데이터 가져오기
                    boolean newState = !clickedTodo.isCompleted(); // 새로운 완료 상태 계산

                        // 상태 변경 및 관련 시간 정보 초기화 후 즉시 업데이트 요청
                        clickedTodo.setCompleted(false);
                        viewModel.update(clickedTodo);
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
                    oldItem.isCompleted() == newItem.isCompleted();
        }
    };

}