package com.am.mytodolistapp.ui.collaboration;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.ProjectTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ProjectTaskAdapter extends ListAdapter<ProjectTask, ProjectTaskAdapter.TaskViewHolder> {

    private OnTaskActionListener onToggleCompleteListener;
    private OnTaskActionListener onEditListener;
    private OnTaskActionListener onDeleteListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.KOREAN);

    public interface OnTaskActionListener {
        void onAction(ProjectTask task);
    }

    public ProjectTaskAdapter(OnTaskActionListener onToggleCompleteListener,
                              OnTaskActionListener onEditListener,
                              OnTaskActionListener onDeleteListener) {
        super(DIFF_CALLBACK);
        this.onToggleCompleteListener = onToggleCompleteListener;
        this.onEditListener = onEditListener;
        this.onDeleteListener = onDeleteListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ProjectTask task = getItem(position);
        holder.bind(task);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkboxCompleted;
        private TextView textTaskTitle;
        private TextView textTaskContent;
        private TextView textDueDate;
        private TextView textPriority;
        private ImageButton buttonEdit;
        private ImageButton buttonDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxCompleted = itemView.findViewById(R.id.checkbox_task_completed);
            textTaskTitle = itemView.findViewById(R.id.text_task_title);
            textTaskContent = itemView.findViewById(R.id.text_task_content);
            textDueDate = itemView.findViewById(R.id.text_task_due_date);
            textPriority = itemView.findViewById(R.id.text_task_priority);
            buttonEdit = itemView.findViewById(R.id.button_edit_task);
            buttonDelete = itemView.findViewById(R.id.button_delete_task);

            checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onToggleCompleteListener != null) {
                    onToggleCompleteListener.onAction(getItem(position));
                }
            });

            buttonEdit.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onEditListener != null) {
                    onEditListener.onAction(getItem(position));
                }
            });

            buttonDelete.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDeleteListener != null) {
                    onDeleteListener.onAction(getItem(position));
                }
            });
        }

        public void bind(ProjectTask task) {
            checkboxCompleted.setChecked(task.isCompleted());
            textTaskTitle.setText(task.getTitle());

            // 완료된 작업은 취소선 표시
            if (task.isCompleted()) {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // 내용이 있으면 표시
            if (task.getContent() != null && !task.getContent().isEmpty()) {
                textTaskContent.setVisibility(View.VISIBLE);
                textTaskContent.setText(task.getContent());
            } else {
                textTaskContent.setVisibility(View.GONE);
            }

            // 기한 표시
            if (task.getDueDate() != null) {
                textDueDate.setVisibility(View.VISIBLE);
                Date dueDate = new Date(task.getDueDate());
                textDueDate.setText("기한: " + dateFormat.format(dueDate));

                // 기한 지났으면 빨간색으로 표시
                if (dueDate.before(new Date())) {
                    textDueDate.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                } else {
                    textDueDate.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
                }
            } else {
                textDueDate.setVisibility(View.GONE);
            }

            // 우선순위 표시
            if (task.getPriority() != null) {
                textPriority.setVisibility(View.VISIBLE);
                String priorityText = getPriorityText(task.getPriority());
                textPriority.setText(priorityText);
                textPriority.setTextColor(getPriorityColor(task.getPriority()));
            } else {
                textPriority.setVisibility(View.GONE);
            }
        }

        private String getPriorityText(String priority) {
            switch (priority) {
                case "HIGH":
                    return "높음";
                case "MEDIUM":
                    return "보통";
                case "LOW":
                    return "낮음";
                default:
                    return "보통";
            }
        }

        private int getPriorityColor(String priority) {
            switch (priority) {
                case "HIGH":
                    return itemView.getContext().getColor(android.R.color.holo_red_dark);
                case "MEDIUM":
                    return itemView.getContext().getColor(android.R.color.holo_orange_dark);
                case "LOW":
                    return itemView.getContext().getColor(android.R.color.holo_green_dark);
                default:
                    return itemView.getContext().getColor(android.R.color.darker_gray);
            }
        }
    }

    private static final DiffUtil.ItemCallback<ProjectTask> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ProjectTask>() {
                @Override
                public boolean areItemsTheSame(@NonNull ProjectTask oldItem, @NonNull ProjectTask newItem) {
                    return Objects.equals(oldItem.getTaskId(), newItem.getTaskId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ProjectTask oldItem, @NonNull ProjectTask newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                            Objects.equals(oldItem.getContent(), newItem.getContent()) &&
                            oldItem.isCompleted() == newItem.isCompleted() &&
                            Objects.equals(oldItem.getDueDate(), newItem.getDueDate()) &&
                            Objects.equals(oldItem.getPriority(), newItem.getPriority()) &&
                            oldItem.getUpdatedAt() == newItem.getUpdatedAt();
                }
            };
}