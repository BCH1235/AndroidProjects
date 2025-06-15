package com.am.mytodolistapp.ui.location;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;

public class LocationTaskAdapter extends ListAdapter<TodoItem, LocationTaskAdapter.TaskViewHolder> {

    private final LocationBasedTaskViewModel viewModel;

    public LocationTaskAdapter(LocationBasedTaskViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_todo, parent, false);
        return new TaskViewHolder(itemView, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBoxCompleted;
        private final TextView textViewTitle;
        private final LocationBasedTaskViewModel viewModel;

        public TaskViewHolder(@NonNull View itemView, LocationBasedTaskViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textViewTitle = itemView.findViewById(R.id.text_view_title);

            // 항목 클릭 - 수정
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem todo = ((LocationTaskAdapter)
                            ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);
                    EditLocationTaskDialogFragment dialog = EditLocationTaskDialogFragment.newInstance(todo);
                    if (itemView.getContext() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                        dialog.show(activity.getSupportFragmentManager(), "EditLocationTaskDialog");
                    }
                }
            });

            // 길게 눌러서 삭제 기능 추가
            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem todo = ((LocationTaskAdapter)
                            ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);

                    // 삭제 확인 다이얼로그 표시
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("할 일 삭제")
                            .setMessage("'" + todo.getTitle() + "'을(를) 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (dialog, which) -> {
                                viewModel.deleteTodo(todo);
                                Toast.makeText(itemView.getContext(), "할 일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("취소", null)
                            .show();

                    return true; // 이벤트 소비됨을 표시
                }
                return false;
            });
        }

        public void bind(TodoItem todoItem) {
            textViewTitle.setText(todoItem.getTitle());

            // 완료된 할 일의 텍스트 스타일 변경
            if (todoItem.isCompleted()) {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textViewTitle.setAlpha(0.6f);
            } else {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                textViewTitle.setAlpha(1.0f);
            }

            // 체크박스 상태 설정 (무한 루프 방지를 위해 리스너를 null로 설정 후 상태 변경)
            checkBoxCompleted.setOnCheckedChangeListener(null);
            checkBoxCompleted.setChecked(todoItem.isCompleted());

            // 체크박스 클릭 이벤트
            checkBoxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // 🚨 isChecked 값을 사용하지 않고, ViewModel의 토글 메소드를 직접 호출합니다.
                    // 이렇게 하면 ViewModel이 상태 관리를 책임지게 되어 더 안정적입니다.
                    TodoItem todo = ((LocationTaskAdapter)
                            ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);
                    viewModel.toggleTodoCompletion(todo);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<TodoItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<TodoItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull TodoItem oldItem, @NonNull TodoItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TodoItem oldItem, @NonNull TodoItem newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.isCompleted() == newItem.isCompleted() &&
                    oldItem.getUpdatedAt() == newItem.getUpdatedAt();
        }
    };
}