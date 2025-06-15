package com.am.mytodolistapp.ui.task;

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
        View itemView = inflater.inflate(R.layout.list_item_todo, parent, false);
        return new TaskViewHolder(itemView, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskListViewModel.TodoWithCategory currentTodoWithCategory = getItem(position);
        holder.bind(currentTodoWithCategory);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBoxCompleted;
        private final TextView textViewTitle;
        private final TaskListViewModel viewModel;

        public TaskViewHolder(@NonNull View itemView, TaskListViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textViewTitle = itemView.findViewById(R.id.text_view_title);

            // 항목 전체 클릭 이벤트 처리
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TaskListViewModel.TodoWithCategory todoWithCategory =
                            ((TaskListAdapter) ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);
                    TodoItem clickedTodo = todoWithCategory.getTodoItem();

                    EditTodoDialogFragment dialogFragment = EditTodoDialogFragment.newInstance(clickedTodo);
                    if (itemView.getContext() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                        dialogFragment.show(activity.getSupportFragmentManager(), "EditTodoDialog");
                    } else {
                        Log.e("TaskListAdapter", "Cannot get FragmentManager from context");
                    }
                }
            });
        }

        public void bind(TaskListViewModel.TodoWithCategory todoWithCategory) {
            TodoItem todoItem = todoWithCategory.getTodoItem();

            // 기본 정보 설정
            textViewTitle.setText(todoItem.getTitle());

            // 체크박스 상태 설정
            checkBoxCompleted.setOnCheckedChangeListener(null);
            checkBoxCompleted.setChecked(todoItem.isCompleted());

            // 체크박스 리스너 설정
            // TaskListAdapter.java의 bind 메서드에서
            checkBoxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TodoItem clickedTodo = todoItem;
                    clickedTodo.setCompleted(isChecked);
                    viewModel.update(clickedTodo); // 자동으로 완료율 업데이트 트리거
                }
            });

            // 카테고리 정보가 있으면 제목에 추가 표시
            String categoryName = todoWithCategory.getCategoryName();
            if (categoryName != null && !categoryName.isEmpty()) {
                textViewTitle.setText(todoItem.getTitle() + " [" + categoryName + "]");
            } else {
                textViewTitle.setText(todoItem.getTitle());
            }
        }
    }

    private static final DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory> DIFF_CALLBACK = new DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
            return oldItem.getTodoItem().getId() == newItem.getTodoItem().getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
            TodoItem oldTodo = oldItem.getTodoItem();
            TodoItem newTodo = newItem.getTodoItem();

            return oldTodo.getTitle().equals(newTodo.getTitle()) &&
                    oldTodo.isCompleted() == newTodo.isCompleted() &&
                    ((oldTodo.getCategoryId() == null && newTodo.getCategoryId() == null) ||
                            (oldTodo.getCategoryId() != null && oldTodo.getCategoryId().equals(newTodo.getCategoryId()))) &&
                    ((oldItem.getCategoryName() == null && newItem.getCategoryName() == null) ||
                            (oldItem.getCategoryName() != null && oldItem.getCategoryName().equals(newItem.getCategoryName())));
        }
    };
}