package com.am.mytodolistapp.ui.task;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd (E)", Locale.KOREAN);

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

    // ViewHolder 클래스
    class GroupViewHolder extends RecyclerView.ViewHolder {
        private TextView textGroupTitle;
        private ImageView imageExpandArrow;
        private RecyclerView recyclerViewTasksInGroup;
        private TaskAdapter taskAdapter;
        private View groupHeader;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            // 레이아웃 파일의 ID와 일치시킴
            groupHeader = itemView.findViewById(R.id.layout_group_header);
            textGroupTitle = itemView.findViewById(R.id.text_group_title);
            imageExpandArrow = itemView.findViewById(R.id.image_expand_arrow);
            recyclerViewTasksInGroup = itemView.findViewById(R.id.recycler_view_tasks_in_group);

            // 내부 RecyclerView 설정
            recyclerViewTasksInGroup.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            taskAdapter = new TaskAdapter();
            recyclerViewTasksInGroup.setAdapter(taskAdapter);
        }

        public void bind(TaskGroup group) {
            textGroupTitle.setText(String.format(Locale.getDefault(), "%s (%d)", group.getTitle(), group.getTasks().size()));
            taskAdapter.submitList(group.getTasks());

            // 확장/축소 상태에 따른 UI 초기 설정
            updateExpandCollapseUI(group.isExpanded(), false);

            // 그룹 헤더 클릭 이벤트
            groupHeader.setOnClickListener(v -> {
                group.setExpanded(!group.isExpanded());
                updateExpandCollapseUI(group.isExpanded(), true);
            });
        }

        // 확장/축소 UI 업데이트
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

    // 개별 할 일 어댑터 (내부 클래스)
    private class TaskAdapter extends ListAdapter<TaskListViewModel.TodoWithCategory, TaskAdapter.TaskViewHolder> {

        public TaskAdapter() {
            super(new TaskDiffCallback());
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_todo_with_collaboration, parent, false);
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
            private TextView textContent;
            private TextView textDueDate;
            private TextView textCategory;

            private ImageView iconCollaboration;
            private TextView textProjectName;
            private TextView textPriority;
            private View priorityIndicator;
            private TextView textAssignedTo;
            private View layoutCollaborationInfo;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);

                checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
                textTitle = itemView.findViewById(R.id.text_title);
                textContent = itemView.findViewById(R.id.text_content);
                textDueDate = itemView.findViewById(R.id.text_due_date);
                textCategory = itemView.findViewById(R.id.text_category);

                iconCollaboration = itemView.findViewById(R.id.icon_collaboration);
                textProjectName = itemView.findViewById(R.id.text_project_name);
                textPriority = itemView.findViewById(R.id.text_priority);
                priorityIndicator = itemView.findViewById(R.id.priority_indicator);
                textAssignedTo = itemView.findViewById(R.id.text_assigned_to);
                layoutCollaborationInfo = itemView.findViewById(R.id.layout_collaboration_info);
            }

            public void bind(TaskListViewModel.TodoWithCategory todoWithCategory) {
                TodoItem todo = todoWithCategory.getTodoItem();

                textTitle.setText(todo.getTitle());
                checkBoxCompleted.setChecked(todo.isCompleted());

                if (todo.getContent() != null && !todo.getContent().isEmpty()) {
                    textContent.setVisibility(View.VISIBLE);
                    textContent.setText(todo.getContent());
                } else {
                    textContent.setVisibility(View.GONE);
                }

                if (todo.getDueDate() != null) {
                    textDueDate.setVisibility(View.VISIBLE);
                    textDueDate.setText(dateFormat.format(new Date(todo.getDueDate())));

                    if (todo.isOverdue()) {
                        textDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.overdue_indicator));
                    } else if (todo.isDueToday()) {
                        textDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.today_indicator));
                    } else {
                        textDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.future_indicator));
                    }
                } else {
                    textDueDate.setVisibility(View.GONE);
                }

                if (todoWithCategory.getCategoryName() != null) {
                    textCategory.setVisibility(View.VISIBLE);
                    textCategory.setText(todoWithCategory.getCategoryName());
                } else {
                    textCategory.setVisibility(View.GONE);
                }

                if (todo.isFromCollaboration()) {
                    setupCollaborationUI(todo);
                } else {
                    setupLocalUI();
                }

                applyCompletionStyle(todo.isCompleted());
                setupClickListeners(todo);
            }

            private void setupCollaborationUI(TodoItem todo) {
                if (iconCollaboration != null) {
                    iconCollaboration.setVisibility(View.VISIBLE);
                    iconCollaboration.setImageResource(R.drawable.ic_collaboration);
                    iconCollaboration.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.collaboration_primary));
                }
                if (textProjectName != null && todo.getProjectName() != null) {
                    textProjectName.setVisibility(View.VISIBLE);
                    textProjectName.setText(todo.getProjectName());
                    textProjectName.setBackgroundResource(R.drawable.bg_project_tag);
                }
                if (textPriority != null && todo.getPriority() != null) {
                    textPriority.setVisibility(View.VISIBLE);
                    textPriority.setText(todo.getPriorityDisplayText());
                }
                if (priorityIndicator != null) {
                    setupPriorityIndicator(todo.getPriority());
                }
                if (textAssignedTo != null && todo.getAssignedTo() != null) {
                    textAssignedTo.setVisibility(View.VISIBLE);
                    textAssignedTo.setText("담당: " + todo.getAssignedTo());
                }
                if (layoutCollaborationInfo != null) {
                    layoutCollaborationInfo.setVisibility(View.VISIBLE);
                }
                itemView.setBackgroundResource(R.drawable.bg_collaboration_todo_item);
            }

            private void setupLocalUI() {
                if (iconCollaboration != null) iconCollaboration.setVisibility(View.GONE);
                if (textProjectName != null) textProjectName.setVisibility(View.GONE);
                if (textPriority != null) textPriority.setVisibility(View.GONE);
                if (priorityIndicator != null) priorityIndicator.setVisibility(View.GONE);
                if (textAssignedTo != null) textAssignedTo.setVisibility(View.GONE);
                if (layoutCollaborationInfo != null) layoutCollaborationInfo.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.bg_local_todo_item);
            }

            private void setupPriorityIndicator(String priority) {
                if (priorityIndicator == null || priority == null) return;
                priorityIndicator.setVisibility(View.VISIBLE);
                int colorRes;
                switch (priority.toUpperCase()) {
                    case "HIGH": colorRes = R.color.priority_high; break;
                    case "MEDIUM": colorRes = R.color.priority_medium; break;
                    case "LOW": colorRes = R.color.priority_low; break;
                    default: priorityIndicator.setVisibility(View.GONE); return;
                }
                priorityIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), colorRes));
            }

            private void applyCompletionStyle(boolean isCompleted) {
                if (isCompleted) {
                    textTitle.setPaintFlags(textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    textTitle.setAlpha(0.6f);
                    if (textContent.getVisibility() == View.VISIBLE) textContent.setAlpha(0.6f);
                    itemView.setAlpha(0.7f);
                } else {
                    textTitle.setPaintFlags(textTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    textTitle.setAlpha(1.0f);
                    if (textContent.getVisibility() == View.VISIBLE) textContent.setAlpha(1.0f);
                    itemView.setAlpha(1.0f);
                }
            }

            private void setupClickListeners(TodoItem todo) {
                checkBoxCompleted.setOnClickListener(v -> {
                    if (viewModel != null) viewModel.toggleCompletion(todo);
                });
                itemView.setOnClickListener(v -> {
                    // EditTodoDialogFragment.newInstance(todo).show(...);
                });
                itemView.setOnLongClickListener(v -> {
                    if (viewModel != null) {
                        viewModel.delete(todo);
                        return true;
                    }
                    return false;
                });
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
            return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                    oldItem.getTasks().size() == newItem.getTasks().size() &&
                    oldItem.isExpanded() == newItem.isExpanded();
        }
    }

    private static class TaskDiffCallback extends DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory> {
        @Override
        public boolean areItemsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
            return oldItem.getTodoItem().getId() == newItem.getTodoItem().getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem, @NonNull TaskListViewModel.TodoWithCategory newItem) {
            TodoItem oldTodo = oldItem.getTodoItem();
            TodoItem newTodo = newItem.getTodoItem();
            return Objects.equals(oldTodo.getTitle(), newTodo.getTitle()) &&
                    oldTodo.isCompleted() == newTodo.isCompleted() &&
                    Objects.equals(oldTodo.getContent(), newTodo.getContent()) &&
                    Objects.equals(oldTodo.getDueDate(), newTodo.getDueDate()) &&
                    oldTodo.isFromCollaboration() == newTodo.isFromCollaboration() &&
                    Objects.equals(oldTodo.getProjectName(), newTodo.getProjectName()) &&
                    Objects.equals(oldTodo.getPriority(), newTodo.getPriority()) &&
                    Objects.equals(oldItem.getCategoryName(), newItem.getCategoryName());
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
            this.isExpanded = true; // 기본적으로 확장된 상태
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public List<TaskListViewModel.TodoWithCategory> getTasks() { return tasks; }
        public boolean isExpanded() { return isExpanded; }
        public void setExpanded(boolean expanded) { isExpanded = expanded; }
    }
}