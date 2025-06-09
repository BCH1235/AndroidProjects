package com.am.mytodolistapp.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CollaborationTodoItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CollaborationTodoAdapter extends ListAdapter<CollaborationTodoItem, CollaborationTodoAdapter.TodoViewHolder> {

    private final CollaborationViewModel viewModel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd", Locale.getDefault());

    public CollaborationTodoAdapter(CollaborationViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collaboration_todo, parent, false);
        return new TodoViewHolder(view, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkboxCompleted;
        private final TextView textTodoTitle;
        private final TextView textTodoContent;
        private final TextView textCreatedBy;
        private final TextView textAssignedTo;
        private final TextView textPriority;
        private final TextView textDueDate;
        private final ImageButton buttonEdit;
        private final CollaborationViewModel viewModel;

        public TodoViewHolder(@NonNull View itemView, CollaborationViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            checkboxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textTodoTitle = itemView.findViewById(R.id.text_todo_title);
            textTodoContent = itemView.findViewById(R.id.text_todo_content);
            textCreatedBy = itemView.findViewById(R.id.text_created_by);
            textAssignedTo = itemView.findViewById(R.id.text_assigned_to);
            textPriority = itemView.findViewById(R.id.text_priority);
            textDueDate = itemView.findViewById(R.id.text_due_date);
            buttonEdit = itemView.findViewById(R.id.button_edit);

            // 할일 편집 버튼
            buttonEdit.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CollaborationTodoItem todo = ((CollaborationTodoAdapter)
                            ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);

                    EditCollaborationTodoDialogFragment dialog =
                            EditCollaborationTodoDialogFragment.newInstance(todo);
                    if (itemView.getContext() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                        dialog.show(activity.getSupportFragmentManager(), "EditTodoDialog");
                    }
                }
            });
        }

        public void bind(CollaborationTodoItem todo) {
            textTodoTitle.setText(todo.getTitle());

            // 내용 표시
            if (todo.getContent() != null && !todo.getContent().isEmpty()) {
                textTodoContent.setText(todo.getContent());
                textTodoContent.setVisibility(View.VISIBLE);
            } else {
                textTodoContent.setVisibility(View.GONE);
            }

            // 생성자 정보
            textCreatedBy.setText("생성: " + todo.getCreatedByName());

            // 할당자 정보
            if (todo.isAssigned()) {
                textAssignedTo.setText("담당: " + todo.getAssignedToName());
                textAssignedTo.setVisibility(View.VISIBLE);
            } else {
                textAssignedTo.setText("담당자 없음");
                textAssignedTo.setVisibility(View.VISIBLE);
            }

            // 우선순위 표시
            textPriority.setText(todo.getPriorityString());
            switch (todo.getPriority()) {
                case 3: // 높음
                    textPriority.setTextColor(Color.parseColor("#F44336"));
                    break;
                case 2: // 보통
                    textPriority.setTextColor(Color.parseColor("#FF9800"));
                    break;
                case 1: // 낮음
                    textPriority.setTextColor(Color.parseColor("#4CAF50"));
                    break;
            }

            // 기한 표시
            if (todo.getDueDate() != null) {
                String dueDateStr = new SimpleDateFormat("MM.dd", Locale.getDefault())
                        .format(new Date(todo.getDueDate()));
                textDueDate.setText("기한: " + dueDateStr);
                textDueDate.setVisibility(View.VISIBLE);

                // 기한 지났는지 확인
                if (todo.getDueDate() < System.currentTimeMillis() && !todo.isCompleted()) {
                    textDueDate.setTextColor(Color.RED);
                } else {
                    textDueDate.setTextColor(Color.parseColor("#666666"));
                }
            } else {
                textDueDate.setVisibility(View.GONE);
            }

            // 체크박스 설정
            checkboxCompleted.setOnCheckedChangeListener(null);
            checkboxCompleted.setChecked(todo.isCompleted());
            checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    viewModel.completeTodo(todo);
                } else {
                    todo.setCompleted(false);
                    viewModel.updateTodo(todo);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<CollaborationTodoItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CollaborationTodoItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CollaborationTodoItem oldItem, @NonNull CollaborationTodoItem newItem) {
                    return oldItem.getTodoId().equals(newItem.getTodoId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CollaborationTodoItem oldItem, @NonNull CollaborationTodoItem newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle()) &&
                            oldItem.isCompleted() == newItem.isCompleted() &&
                            oldItem.getUpdatedAt() == newItem.getUpdatedAt();
                }
            };
}