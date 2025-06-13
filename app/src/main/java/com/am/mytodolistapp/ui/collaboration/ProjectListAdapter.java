package com.am.mytodolistapp.ui.collaboration;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.Project;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProjectListAdapter extends ListAdapter<Project, ProjectListAdapter.ProjectViewHolder> {

    private OnProjectClickListener onProjectClickListener;
    private OnInviteMemberClickListener onInviteMemberClickListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN);

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public interface OnInviteMemberClickListener {
        void onInviteMemberClick(Project project);
    }

    public ProjectListAdapter(OnProjectClickListener onProjectClickListener,
                              OnInviteMemberClickListener onInviteMemberClickListener) {
        super(DIFF_CALLBACK);
        this.onProjectClickListener = onProjectClickListener;
        this.onInviteMemberClickListener = onInviteMemberClickListener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = getItem(position);
        holder.bind(project);
    }

    @Override
    public void submitList(@Nullable List<Project> list) {
        Log.d("ProjectListAdapter", "submitList called with " + (list != null ? list.size() : "null") + " items");
        super.submitList(list);
    }


    class ProjectViewHolder extends RecyclerView.ViewHolder {
        private TextView textProjectName;
        private TextView textProjectDescription;
        private TextView textMemberCount;
        private TextView textCreatedDate;
        private ImageButton buttonInviteMember;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            textProjectName = itemView.findViewById(R.id.text_project_name);
            textProjectDescription = itemView.findViewById(R.id.text_project_description);
            textMemberCount = itemView.findViewById(R.id.text_member_count);
            textCreatedDate = itemView.findViewById(R.id.text_created_date);
            buttonInviteMember = itemView.findViewById(R.id.button_invite_member);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onProjectClickListener != null) {
                    onProjectClickListener.onProjectClick(getItem(position));
                }
            });

            buttonInviteMember.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onInviteMemberClickListener != null) {
                    onInviteMemberClickListener.onInviteMemberClick(getItem(position));
                }
            });
        }

        public void bind(Project project) {
            textProjectName.setText(project.getProjectName());

            if (project.getDescription() != null && !project.getDescription().isEmpty()) {
                textProjectDescription.setVisibility(View.VISIBLE);
                textProjectDescription.setText(project.getDescription());
            } else {
                textProjectDescription.setVisibility(View.GONE);
            }

            int memberCount = project.getMemberIds() != null ? project.getMemberIds().size() : 1;
            textMemberCount.setText("멤버 " + memberCount + "명");

            Date createdDate = new Date(project.getCreatedAt());
            textCreatedDate.setText("생성일: " + dateFormat.format(createdDate));
        }
    }

    private static final DiffUtil.ItemCallback<Project> DIFF_CALLBACK = new DiffUtil.ItemCallback<Project>() {
        @Override
        public boolean areItemsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
            return Objects.equals(oldItem.getProjectId(), newItem.getProjectId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
            return Objects.equals(oldItem.getProjectName(), newItem.getProjectName()) &&
                    Objects.equals(oldItem.getDescription(), newItem.getDescription()) &&
                    Objects.equals(oldItem.getMemberIds(), newItem.getMemberIds()) &&
                    oldItem.getUpdatedAt() == newItem.getUpdatedAt();
        }
    };
}
