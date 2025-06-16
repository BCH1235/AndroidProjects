package com.am.mytodolistapp.ui.task;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GroupedTaskAdapter extends ListAdapter<GroupedTaskAdapter.TaskGroup, GroupedTaskAdapter.GroupViewHolder> {
    private static final String TAG = "GroupedTaskAdapter";

    private TaskListViewModel viewModel;

    public GroupedTaskAdapter(TaskListViewModel viewModel) {
        super(new GroupDiffCallback());
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        TaskGroup group = getItem(position);
        holder.bind(group);
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private TextView textGroupTitle;
        private ImageView imageExpandArrow;
        private RecyclerView recyclerViewTasksInGroup;
        private TaskAdapter taskAdapter;
        private View groupHeader;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupHeader = itemView.findViewById(R.id.layout_group_header);
            textGroupTitle = itemView.findViewById(R.id.text_group_title);
            imageExpandArrow = itemView.findViewById(R.id.image_expand_arrow);
            recyclerViewTasksInGroup = itemView.findViewById(R.id.recycler_view_tasks_in_group);

            recyclerViewTasksInGroup.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            taskAdapter = new TaskAdapter();
            recyclerViewTasksInGroup.setAdapter(taskAdapter);
        }

        public void bind(TaskGroup group) {
            textGroupTitle.setText(String.format(Locale.getDefault(), "%s (%d)", group.getTitle(), group.getTasks().size()));
            taskAdapter.submitList(group.getTasks());
            updateExpandCollapseUI(group.isExpanded(), false);
            groupHeader.setOnClickListener(v -> {
                group.setExpanded(!group.isExpanded());
                updateExpandCollapseUI(group.isExpanded(), true);
            });
        }

        private void updateExpandCollapseUI(boolean isExpanded, boolean animate) {
            if (isExpanded) {
                recyclerViewTasksInGroup.setVisibility(View.VISIBLE);
                if (animate) {
                    Animation rotate = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.rotate_180);
                    imageExpandArrow.startAnimation(rotate);
                }
                imageExpandArrow.setRotation(180);
            } else {
                recyclerViewTasksInGroup.setVisibility(View.GONE);
                if (animate) {
                    Animation rotate = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.rotate_0);
                    imageExpandArrow.startAnimation(rotate);
                }
                imageExpandArrow.setRotation(0);
            }
        }
    }

    private class TaskAdapter extends ListAdapter<TaskListViewModel.TodoWithCategory, TaskAdapter.TaskViewHolder> {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());

        public TaskAdapter() {
            super(new TaskDiffCallback());
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_todo_unified, parent, false);
            return new TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            TaskListViewModel.TodoWithCategory todoWithCategory = getItem(position);
            holder.bind(todoWithCategory);
        }

        class TaskViewHolder extends RecyclerView.ViewHolder {
            private CheckBox checkBoxCompleted;
            private TextView textTitle;
            private TextView textDetails;
            private ImageButton buttonEdit;
            private ImageButton buttonDelete;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
                textTitle = itemView.findViewById(R.id.text_todo_title);
                textDetails = itemView.findViewById(R.id.text_todo_details);
                buttonEdit = itemView.findViewById(R.id.button_edit_todo);
                buttonDelete = itemView.findViewById(R.id.button_delete_todo);
            }

            public void bind(TaskListViewModel.TodoWithCategory todoWithCategory) {
                TodoItem todo = todoWithCategory.getTodoItem();
                textTitle.setText(todo.getTitle());
                updateDetailsText(todo, todoWithCategory.getCategoryName());
                applyCompletionStyle(todo.isCompleted());
                setupListeners(todo);
            }

            //날짜 정보 표시 로직
            private void updateDetailsText(TodoItem todo, String categoryName) {
                StringBuilder details = new StringBuilder();

                // 협업/카테고리 정보 추가
                if (todo.isFromCollaboration()) {
                    details.append("[").append(todo.getProjectName()).append("] ");
                } else if (categoryName != null) {
                    details.append("[").append(categoryName).append("] ");
                }

                //날짜 정보 표시 로직
                if (todo.getDueDate() != null) {
                    // 기한이 있는 경우: "기한: MM-dd"
                    details.append("기한: ").append(dateFormat.format(new Date(todo.getDueDate())));
                } else {
                    //기한이 없는 경우 생성 날짜 표시 "생성: MM-dd"
                    details.append("생성: ").append(dateFormat.format(new Date(todo.getCreatedAt())));
                }

                // 항상 세부 정보를 표시
                textDetails.setText(details.toString().trim());
                textDetails.setVisibility(View.VISIBLE);
            }

            private void applyCompletionStyle(boolean isCompleted) {
                if (isCompleted) {
                    textTitle.setPaintFlags(textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    textTitle.setAlpha(0.6f);
                    textDetails.setAlpha(0.6f);
                } else {
                    textTitle.setPaintFlags(textTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    textTitle.setAlpha(1.0f);
                    textDetails.setAlpha(1.0f);
                }
                checkBoxCompleted.setChecked(isCompleted);
            }

            private void setupListeners(TodoItem todo) {
                checkBoxCompleted.setOnClickListener(v -> {
                    if (viewModel != null) viewModel.toggleCompletion(todo);
                });

                buttonEdit.setOnClickListener(v -> openEditDialog(todo));

                buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog(todo));
            }

            private void openEditDialog(TodoItem todo) {
                if (itemView.getContext() instanceof AppCompatActivity) {
                    EditTodoDialogFragment dialog = EditTodoDialogFragment.newInstance(todo);
                    dialog.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "EditTodoDialog");
                }
            }

            private void showDeleteConfirmationDialog(TodoItem todo) {
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("할 일 삭제")
                        .setMessage("'" + todo.getTitle() + "' 항목을 삭제하시겠습니까?")
                        .setPositiveButton("삭제", (dialog, which) -> {
                            if (viewModel != null) {
                                viewModel.delete(todo);
                                Toast.makeText(itemView.getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("취소", null)
                        .show();
            }
        }
    }

    private static class GroupDiffCallback extends DiffUtil.ItemCallback<TaskGroup> {
        @Override
        public boolean areItemsTheSame(@NonNull TaskGroup oldItem, @NonNull TaskGroup newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull TaskGroup oldItem, @NonNull TaskGroup newItem) {
            return oldItem.equals(newItem);
        }
    }

    private static class TaskDiffCallback extends DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory> {
        @Override
        public boolean areItemsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
            return oldItem.getTodoItem().getId() == newItem.getTodoItem().getId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
            return oldItem.equals(newItem);
        }
    }

    public static class TaskGroup {
        private final String id;
        private final String title;
        private final List<TaskListViewModel.TodoWithCategory> tasks;
        private boolean isExpanded;

        public TaskGroup(String id, String title, List<TaskListViewModel.TodoWithCategory> tasks) {
            this.id = id;
            this.title = title;
            this.tasks = tasks;
            this.isExpanded = true;
        }
        public String getId() { return id; }
        public String getTitle() { return title; }
        public List<TaskListViewModel.TodoWithCategory> getTasks() { return tasks; }
        public boolean isExpanded() { return isExpanded; }
        public void setExpanded(boolean expanded) { isExpanded = expanded; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TaskGroup taskGroup = (TaskGroup) o;
            return isExpanded == taskGroup.isExpanded && Objects.equals(id, taskGroup.id) && Objects.equals(title, taskGroup.title) && Objects.equals(tasks, taskGroup.tasks);
        }
        @Override
        public int hashCode() {
            return Objects.hash(id, title, tasks, isExpanded);
        }
    }
}