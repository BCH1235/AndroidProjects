package com.am.mytodolistapp.ui;

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

            // 항목 클릭
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

            // 체크박스 상태 설정
            checkBoxCompleted.setOnCheckedChangeListener(null);
            checkBoxCompleted.setChecked(todoItem.isCompleted());

            // 체크박스 클릭 이벤트
            checkBoxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem todo = ((LocationTaskAdapter)
                            ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);

                    // 새로운 상태로 설정
                    todo.setCompleted(isChecked);
                    viewModel.updateTodo(todo);
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
                    oldItem.isCompleted() == newItem.isCompleted();
        }
    };
}