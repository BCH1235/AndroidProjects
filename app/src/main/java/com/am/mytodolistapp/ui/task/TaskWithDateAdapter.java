package com.am.mytodolistapp.ui.task;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
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

public class TaskWithDateAdapter extends ListAdapter<TaskListViewModel.TodoWithCategory, TaskWithDateAdapter.TaskViewHolder> {

    private final TaskListViewModel viewModel;

    public TaskWithDateAdapter(TaskListViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // [수정] 새로운 통합 레이아웃 사용
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo_unified, parent, false);
        return new TaskViewHolder(view, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskListViewModel.TodoWithCategory todoWithCategory = getItem(position);
        holder.bind(todoWithCategory);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBoxCompleted;
        private final TextView textTodoTitle;
        private final TextView textTodoDetails;
        private final ImageButton buttonEditTodo;
        private final ImageButton buttonDeleteTodo;
        private final TaskListViewModel viewModel;

        public TaskViewHolder(@NonNull View itemView, TaskListViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;
            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textTodoTitle = itemView.findViewById(R.id.text_todo_title);
            textTodoDetails = itemView.findViewById(R.id.text_todo_details);
            buttonEditTodo = itemView.findViewById(R.id.button_edit_todo);
            buttonDeleteTodo = itemView.findViewById(R.id.button_delete_todo);
        }

        public void bind(TaskListViewModel.TodoWithCategory todoWithCategory) {
            TodoItem todo = todoWithCategory.getTodoItem();

            textTodoTitle.setText(todo.getTitle());
            updateDetailsText(todoWithCategory);
            applyCompletionStyle(todo.isCompleted());
            setupListeners(todo);
        }

        private void updateDetailsText(TaskListViewModel.TodoWithCategory todoWithCategory) {
            if (todoWithCategory.getCategoryName() != null) {
                textTodoDetails.setText("[" + todoWithCategory.getCategoryName() + "]");
                textTodoDetails.setVisibility(View.VISIBLE);
            } else {
                textTodoDetails.setVisibility(View.GONE);
            }
        }

        private void applyCompletionStyle(boolean isCompleted) {
            if (isCompleted) {
                textTodoTitle.setPaintFlags(textTodoTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textTodoTitle.setAlpha(0.6f);
                textTodoDetails.setAlpha(0.6f);
            } else {
                textTodoTitle.setPaintFlags(textTodoTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                textTodoTitle.setAlpha(1.0f);
                textTodoDetails.setAlpha(1.0f);
            }
            checkBoxCompleted.setChecked(isCompleted);
        }

        private void setupListeners(TodoItem todo) {
            checkBoxCompleted.setOnClickListener(v -> viewModel.toggleCompletion(todo));

            buttonEditTodo.setOnClickListener(v -> {
                EditTodoDialogFragment dialogFragment = EditTodoDialogFragment.newInstance(todo);
                if (itemView.getContext() instanceof AppCompatActivity) {
                    dialogFragment.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "EditTodoDialog");
                }
            });

            buttonDeleteTodo.setOnClickListener(v -> showDeleteConfirmationDialog(todo));
        }

        private void showDeleteConfirmationDialog(TodoItem todo) {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("할 일 삭제")
                    .setMessage("'" + todo.getTitle() + "'을(를) 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        viewModel.delete(todo);
                        Toast.makeText(itemView.getContext(), "할 일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    private static final DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory>() {
                @Override
                public boolean areItemsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
                    return oldItem.getTodoItem().getId() == newItem.getTodoItem().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
                    return oldItem.equals(newItem);
                }
            };
}