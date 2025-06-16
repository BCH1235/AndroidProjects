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

import java.util.Objects;

public class TaskListAdapter extends ListAdapter<TaskListViewModel.TodoWithCategory, TaskListAdapter.TaskViewHolder> {

    private final TaskListViewModel viewModel;

    public TaskListAdapter(TaskListViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        // [수정] 새로운 통합 레이아웃 사용
        View itemView = inflater.inflate(R.layout.item_todo_unified, parent, false);
        return new TaskViewHolder(itemView, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskListViewModel.TodoWithCategory currentTodoWithCategory = getItem(position);
        holder.bind(currentTodoWithCategory);
    }

    // [수정] ViewHolder 전체 수정
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBoxCompleted;
        private final TextView textViewTitle;
        private final TextView textViewDetails;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;
        private final TaskListViewModel viewModel;

        public TaskViewHolder(@NonNull View itemView, TaskListViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textViewTitle = itemView.findViewById(R.id.text_todo_title);
            textViewDetails = itemView.findViewById(R.id.text_todo_details);
            buttonEdit = itemView.findViewById(R.id.button_edit_todo);
            buttonDelete = itemView.findViewById(R.id.button_delete_todo);
        }

        public void bind(TaskListViewModel.TodoWithCategory todoWithCategory) {
            TodoItem todoItem = todoWithCategory.getTodoItem();

            textViewTitle.setText(todoItem.getTitle());
            updateDetailsText(todoWithCategory);
            applyCompletionStyle(todoItem.isCompleted());
            setupListeners(todoItem);
        }

        private void updateDetailsText(TaskListViewModel.TodoWithCategory todoWithCategory) {
            if (todoWithCategory.getCategoryName() != null) {
                textViewDetails.setText("[" + todoWithCategory.getCategoryName() + "]");
                textViewDetails.setVisibility(View.VISIBLE);
            } else {
                textViewDetails.setVisibility(View.GONE);
            }
        }

        private void applyCompletionStyle(boolean isCompleted) {
            if (isCompleted) {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textViewTitle.setAlpha(0.6f);
                textViewDetails.setAlpha(0.6f);
            } else {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                textViewTitle.setAlpha(1.0f);
                textViewDetails.setAlpha(1.0f);
            }
            checkBoxCompleted.setChecked(isCompleted);
        }

        private void setupListeners(TodoItem todoItem) {
            checkBoxCompleted.setOnClickListener(v -> viewModel.toggleCompletion(todoItem));

            buttonEdit.setOnClickListener(v -> {
                EditTodoDialogFragment dialogFragment = EditTodoDialogFragment.newInstance(todoItem);
                if (itemView.getContext() instanceof AppCompatActivity) {
                    dialogFragment.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "EditTodoDialog");
                }
            });

            buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog(todoItem));
        }

        private void showDeleteConfirmationDialog(TodoItem todo) {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("할 일 삭제")
                    .setMessage("'" + todo.getTitle() + "' 항목을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        viewModel.delete(todo);
                        Toast.makeText(itemView.getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    private static final DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory> DIFF_CALLBACK = new DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
            return oldItem.getTodoItem().getId() == newItem.getTodoItem().getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
            // 더 정확한 비교를 위해 equals 사용
            return Objects.equals(oldItem, newItem);
        }
    };
}