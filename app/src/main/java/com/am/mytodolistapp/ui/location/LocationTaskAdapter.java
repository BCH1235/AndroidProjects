package com.am.mytodolistapp.ui.location;

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

public class LocationTaskAdapter extends ListAdapter<TodoItem, LocationTaskAdapter.TaskViewHolder> {

    private final LocationBasedTaskViewModel viewModel;

    public LocationTaskAdapter(LocationBasedTaskViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // [수정] 새로운 통합 레이아웃 사용
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo_unified, parent, false);
        return new TaskViewHolder(itemView, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBoxCompleted;
        private final TextView textTitle;
        private final TextView textDetails;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;
        private final LocationBasedTaskViewModel viewModel;

        public TaskViewHolder(@NonNull View itemView, LocationBasedTaskViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;
            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textTitle = itemView.findViewById(R.id.text_todo_title);
            textDetails = itemView.findViewById(R.id.text_todo_details);
            buttonEdit = itemView.findViewById(R.id.button_edit_todo);
            buttonDelete = itemView.findViewById(R.id.button_delete_todo);
        }

        public void bind(TodoItem todoItem) {
            textTitle.setText(todoItem.getTitle());
            textDetails.setVisibility(View.GONE); // 위치 할 일은 별도 상세 정보 없음
            applyCompletionStyle(todoItem.isCompleted());
            setupListeners(todoItem);
        }

        private void applyCompletionStyle(boolean isCompleted) {
            if (isCompleted) {
                textTitle.setPaintFlags(textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textTitle.setAlpha(0.6f);
            } else {
                textTitle.setPaintFlags(textTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                textTitle.setAlpha(1.0f);
            }
            checkBoxCompleted.setChecked(isCompleted);
        }

        private void setupListeners(TodoItem todoItem) {
            checkBoxCompleted.setOnClickListener(v -> viewModel.toggleTodoCompletion(todoItem));

            buttonEdit.setOnClickListener(v -> {
                EditLocationTaskDialogFragment dialog = EditLocationTaskDialogFragment.newInstance(todoItem);
                if (itemView.getContext() instanceof AppCompatActivity) {
                    dialog.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "EditLocationTaskDialog");
                }
            });

            buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog(todoItem));
        }

        private void showDeleteConfirmationDialog(TodoItem todo) {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("할 일 삭제")
                    .setMessage("'" + todo.getTitle() + "'을(를) 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        viewModel.deleteTodo(todo);
                        Toast.makeText(itemView.getContext(), "할 일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    private static final DiffUtil.ItemCallback<TodoItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<TodoItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull TodoItem oldItem, @NonNull TodoItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TodoItem oldItem, @NonNull TodoItem newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                    oldItem.isCompleted() == newItem.isCompleted() &&
                    oldItem.getUpdatedAt() == newItem.getUpdatedAt();
        }
    };
}