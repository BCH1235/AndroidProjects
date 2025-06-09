package com.am.mytodolistapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.ProjectMember;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProjectMemberAdapter extends ListAdapter<ProjectMember, ProjectMemberAdapter.MemberViewHolder> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());

    public ProjectMemberAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMemberName;
        private final TextView textMemberEmail;
        private final TextView textMemberRole;
        private final TextView textJoinedDate;
        private final ImageView imageMemberAvatar;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            textMemberName = itemView.findViewById(R.id.text_member_name);
            textMemberEmail = itemView.findViewById(R.id.text_member_email);
            textMemberRole = itemView.findViewById(R.id.text_member_role);
            textJoinedDate = itemView.findViewById(R.id.text_joined_date);
            imageMemberAvatar = itemView.findViewById(R.id.image_member_avatar);
        }

        public void bind(ProjectMember member) {
            textMemberName.setText(member.getUserName());
            textMemberEmail.setText(member.getUserEmail());

            // 역할 표시
            String role = "";
            switch (member.getRole()) {
                case "owner":
                    role = "소유자";
                    break;
                case "admin":
                    role = "관리자";
                    break;
                case "member":
                    role = "멤버";
                    break;
            }
            textMemberRole.setText(role);

            // 참여 날짜
            String joinedDate = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    .format(new Date(member.getJoinedAt()));
            textJoinedDate.setText("참여: " + joinedDate);

            // 아바타 (첫 글자로 대체)
            String firstChar = member.getUserName().substring(0, 1).toUpperCase();
            // 실제로는 CircularImageView나 커스텀 뷰를 사용해서 원형 아바타 만들 수 있음
        }
    }

    private static final DiffUtil.ItemCallback<ProjectMember> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ProjectMember>() {
                @Override
                public boolean areItemsTheSame(@NonNull ProjectMember oldItem, @NonNull ProjectMember newItem) {
                    return oldItem.getUserId().equals(newItem.getUserId()) &&
                            oldItem.getProjectId().equals(newItem.getProjectId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ProjectMember oldItem, @NonNull ProjectMember newItem) {
                    return oldItem.getUserName().equals(newItem.getUserName()) &&
                            oldItem.getRole().equals(newItem.getRole()) &&
                            oldItem.getInvitationStatus().equals(newItem.getInvitationStatus());
                }
            };
}
