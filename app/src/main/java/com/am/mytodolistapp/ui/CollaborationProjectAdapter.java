package com.am.mytodolistapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.CollaborationProject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CollaborationProjectAdapter extends ListAdapter<CollaborationProject, CollaborationProjectAdapter.ProjectViewHolder> {

    private final CollaborationViewModel viewModel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());

    public CollaborationProjectAdapter(CollaborationViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collaboration_project, parent, false);
        return new ProjectViewHolder(view, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView textProjectName;
        private final TextView textProjectDescription;
        private final TextView textOwnerName;
        private final TextView textMemberCount;
        private final TextView textLastUpdated;
        private final ImageView imageProjectIcon;
        private final CollaborationViewModel viewModel;

        public ProjectViewHolder(@NonNull View itemView, CollaborationViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            textProjectName = itemView.findViewById(R.id.text_project_name);
            textProjectDescription = itemView.findViewById(R.id.text_project_description);
            textOwnerName = itemView.findViewById(R.id.text_owner_name);
            textMemberCount = itemView.findViewById(R.id.text_member_count);
            textLastUpdated = itemView.findViewById(R.id.text_last_updated);
            imageProjectIcon = itemView.findViewById(R.id.image_project_icon);

            // 프로젝트 클릭 시 프로젝트 상세 화면으로 이동
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CollaborationProject project = ((CollaborationProjectAdapter)
                            ((RecyclerView) itemView.getParent()).getAdapter()).getItem(position);

                    ProjectDetailFragment fragment = ProjectDetailFragment.newInstance(project.getProjectId());
                    if (itemView.getContext() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                        activity.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            });
        }

        public void bind(CollaborationProject project) {
            textProjectName.setText(project.getName());

            if (project.getDescription() != null && !project.getDescription().isEmpty()) {
                textProjectDescription.setText(project.getDescription());
                textProjectDescription.setVisibility(View.VISIBLE);
            } else {
                textProjectDescription.setVisibility(View.GONE);
            }

            textOwnerName.setText("생성자: " + project.getOwnerName());
            textMemberCount.setText("멤버 " + project.getMemberCount() + "명");

            String lastUpdated = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    .format(new Date(project.getUpdatedAt()));
            textLastUpdated.setText("최근 업데이트: " + lastUpdated);

            // 프로젝트 아이콘 설정 (첫 글자 기반)
            String firstChar = project.getName().substring(0, 1).toUpperCase();
            // 여기서는 간단히 텍스트로 처리, 실제로는 원형 배경에 글자를 표시하는 커스텀 뷰 사용 가능
        }
    }

    private static final DiffUtil.ItemCallback<CollaborationProject> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CollaborationProject>() {
                @Override
                public boolean areItemsTheSame(@NonNull CollaborationProject oldItem, @NonNull CollaborationProject newItem) {
                    return oldItem.getProjectId().equals(newItem.getProjectId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CollaborationProject oldItem, @NonNull CollaborationProject newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getUpdatedAt() == newItem.getUpdatedAt() &&
                            oldItem.getMemberCount() == newItem.getMemberCount();
                }
            };
}