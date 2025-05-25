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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        private final TextView textViewEstimatedTime;
        private final TextView textViewActualTime;
        private final TextView textViewCompletionTime;
        private final LocationBasedTaskViewModel viewModel;

        public TaskViewHolder(@NonNull View itemView, LocationBasedTaskViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewEstimatedTime = itemView.findViewById(R.id.text_view_estimated_time);
            textViewActualTime = itemView.findViewById(R.id.text_view_actual_time);
            textViewCompletionTime = itemView.findViewById(R.id.text_view_completion_time);

            // 체크박스 클릭 이벤트
            checkBoxCompleted.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem todo = ((LocationTaskAdapter)
                            ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);
                    boolean newState = !todo.isCompleted();

                    if (newState) {
                        // 완료 처리 - 실제 시간 입력 다이얼로그
                        ActualTimeInputDialogFragment dialog =
                                ActualTimeInputDialogFragment.newInstance(todo.getId());
                        if (itemView.getContext() instanceof AppCompatActivity) {
                            AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                            dialog.show(activity.getSupportFragmentManager(), "ActualTimeInputDialog");
                        }
                    } else {
                        // 미완료로 변경
                        todo.setCompleted(false);
                        todo.setActualTimeMinutes(0);
                        todo.setCompletionTimestamp(0);
                        viewModel.updateTodo(todo);
                    }
                }
            });

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
        }

        public void bind(TodoItem todoItem) {
            textViewTitle.setText(todoItem.getTitle());
            checkBoxCompleted.setChecked(todoItem.isCompleted());

            // 예상 시간 표시
            int estimatedMinutes = todoItem.getEstimatedTimeMinutes();
            if (estimatedMinutes > 0) {
                textViewEstimatedTime.setText("예상: " + formatMinutes(estimatedMinutes));
                textViewEstimatedTime.setVisibility(View.VISIBLE);
            } else {
                textViewEstimatedTime.setVisibility(View.GONE);
            }

            // 완료 상태에 따른 표시
            if (todoItem.isCompleted()) {
                int actualMinutes = todoItem.getActualTimeMinutes();

                if (actualMinutes > 0) {
                    textViewActualTime.setText("실제: " + formatMinutes(actualMinutes));

                    // 시간 비교 색상
                    int colorResId;
                    if (estimatedMinutes <= 0) {
                        colorResId = R.color.text_secondary_default;
                    } else if (actualMinutes < estimatedMinutes) {
                        colorResId = R.color.time_faster;
                    } else if (actualMinutes > estimatedMinutes) {
                        colorResId = R.color.time_slower;
                    } else {
                        colorResId = R.color.time_same;
                    }
                    textViewActualTime.setTextColor(ContextCompat.getColor(itemView.getContext(), colorResId));
                    textViewActualTime.setVisibility(View.VISIBLE);
                } else {
                    textViewActualTime.setVisibility(View.GONE);
                }

                // 완료 시각
                long timestamp = todoItem.getCompletionTimestamp();
                if (timestamp > 0) {
                    textViewCompletionTime.setText("완료: " + formatTimestamp(timestamp));
                    textViewCompletionTime.setVisibility(View.VISIBLE);
                } else {
                    textViewCompletionTime.setVisibility(View.GONE);
                }
            } else {
                textViewActualTime.setVisibility(View.GONE);
                textViewCompletionTime.setVisibility(View.GONE);
            }
        }

        private String formatMinutes(int totalMinutes) {
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

        private String formatTimestamp(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
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
                    oldItem.getEstimatedTimeMinutes() == newItem.getEstimatedTimeMinutes() &&
                    oldItem.getActualTimeMinutes() == newItem.getActualTimeMinutes() &&
                    oldItem.getCompletionTimestamp() == newItem.getCompletionTimestamp();
        }
    };
}