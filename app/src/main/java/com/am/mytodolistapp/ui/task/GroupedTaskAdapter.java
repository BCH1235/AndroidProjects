package com.am.mytodolistapp.ui.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GroupedTaskAdapter extends ListAdapter<GroupedTaskAdapter.TaskGroup, GroupedTaskAdapter.GroupViewHolder> {

    private final TaskListViewModel viewModel;
    private final Map<String, Boolean> expansionStates = new HashMap<>(); // 그룹별 확장 상태

    public GroupedTaskAdapter(TaskListViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;

        // 초기 확장 상태 설정 (모든 그룹 확장됨)
        expansionStates.put("previous", true);
        expansionStates.put("today", true);
        expansionStates.put("future", true);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_group, parent, false);
        return new GroupViewHolder(view, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        TaskGroup group = getItem(position);
        holder.bind(group);
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final View layoutGroupHeader;
        private final TextView textGroupName;
        private final ImageView imageExpandArrow;
        private final RecyclerView recyclerViewTasksInGroup;
        private final TaskListViewModel viewModel;
        private TaskWithDateAdapter taskAdapter;

        public GroupViewHolder(@NonNull View itemView, TaskListViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            layoutGroupHeader = itemView.findViewById(R.id.layout_group_header);
            textGroupName = itemView.findViewById(R.id.text_group_name);
            imageExpandArrow = itemView.findViewById(R.id.image_expand_arrow);
            recyclerViewTasksInGroup = itemView.findViewById(R.id.recycler_view_tasks_in_group);

            // 내부 RecyclerView 설정
            recyclerViewTasksInGroup.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            taskAdapter = new TaskWithDateAdapter(viewModel);
            recyclerViewTasksInGroup.setAdapter(taskAdapter);

            // 헤더 클릭 리스너
            layoutGroupHeader.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TaskGroup group = getItem(position);
                    toggleGroupExpansion(group.getGroupType());
                }
            });
        }

        public void bind(TaskGroup group) {
            textGroupName.setText(group.getGroupName());
            taskAdapter.submitList(group.getTasks());

            // 확장 상태에 따른 UI 업데이트
            boolean isExpanded = expansionStates.getOrDefault(group.getGroupType(), true);
            updateExpansionUI(isExpanded);
        }

        private void toggleGroupExpansion(String groupType) {
            boolean currentState = expansionStates.getOrDefault(groupType, true);
            boolean newState = !currentState;
            expansionStates.put(groupType, newState);
            updateExpansionUI(newState);
        }

        private void updateExpansionUI(boolean isExpanded) {
            // 화살표 회전 애니메이션
            float fromDegrees = isExpanded ? 0f : 180f;
            float toDegrees = isExpanded ? 180f : 0f;

            Animation rotateAnimation = AnimationUtils.loadAnimation(itemView.getContext(),
                    isExpanded ? R.anim.rotate_180 : R.anim.rotate_0);
            imageExpandArrow.startAnimation(rotateAnimation);

            // RecyclerView 표시/숨김
            recyclerViewTasksInGroup.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        }
    }

    // 할일 그룹 데이터 클래스
    public static class TaskGroup {
        private final String groupType; // "previous", "today", "future"
        private final String groupName; // "이전의", "오늘", "미래"
        private final List<TaskListViewModel.TodoWithCategory> tasks;

        public TaskGroup(String groupType, String groupName, List<TaskListViewModel.TodoWithCategory> tasks) {
            this.groupType = groupType;
            this.groupName = groupName;
            this.tasks = tasks;
        }

        public String getGroupType() { return groupType; }
        public String getGroupName() { return groupName; }
        public List<TaskListViewModel.TodoWithCategory> getTasks() { return tasks; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TaskGroup taskGroup = (TaskGroup) o;
            return Objects.equals(groupType, taskGroup.groupType) &&
                    Objects.equals(groupName, taskGroup.groupName) &&
                    Objects.equals(tasks, taskGroup.tasks);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupType, groupName, tasks);
        }
    }

    private static final DiffUtil.ItemCallback<TaskGroup> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TaskGroup>() {
                @Override
                public boolean areItemsTheSame(@NonNull TaskGroup oldItem, @NonNull TaskGroup newItem) {
                    return Objects.equals(oldItem.getGroupType(), newItem.getGroupType());
                }

                @Override
                public boolean areContentsTheSame(@NonNull TaskGroup oldItem, @NonNull TaskGroup newItem) {
                    return oldItem.equals(newItem);
                }
            };
}