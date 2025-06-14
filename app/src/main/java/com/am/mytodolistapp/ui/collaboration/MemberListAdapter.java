package com.am.mytodolistapp.ui.collaboration;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.User;

import java.util.ArrayList;
import java.util.List;

public class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.MemberViewHolder> {

    private List<User> members = new ArrayList<>();
    private String projectOwnerId; // 프로젝트 소유자 ID

    public void setMembers(List<User> members) {
        this.members = members != null ? members : new ArrayList<>();
        sortMembers(); // 관리자를 먼저 정렬
        notifyDataSetChanged();
    }

    public void setProjectOwnerId(String ownerId) {
        this.projectOwnerId = ownerId;
        sortMembers(); // 관리자를 먼저 정렬
        notifyDataSetChanged();
    }

    // 관리자를 맨 위로 정렬하는 메소드
    private void sortMembers() {
        if (projectOwnerId != null && !members.isEmpty()) {
            members.sort((user1, user2) -> {
                boolean isUser1Owner = user1.getUid() != null && user1.getUid().equals(projectOwnerId);
                boolean isUser2Owner = user2.getUid() != null && user2.getUid().equals(projectOwnerId);

                if (isUser1Owner && !isUser2Owner) {
                    return -1; // user1이 관리자면 앞으로
                } else if (!isUser1Owner && isUser2Owner) {
                    return 1;  // user2가 관리자면 뒤로
                } else {
                    // 둘 다 관리자이거나 둘 다 일반 멤버면 이름 순으로 정렬
                    String name1 = user1.getDisplayName() != null ? user1.getDisplayName() :
                            (user1.getEmail() != null ? user1.getEmail() : "");
                    String name2 = user2.getDisplayName() != null ? user2.getDisplayName() :
                            (user2.getEmail() != null ? user2.getEmail() : "");
                    return name1.compareToIgnoreCase(name2);
                }
            });
        }
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = members.get(position);
        holder.bind(member, projectOwnerId);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewMemberIcon;
        private TextView textViewMemberEmail;
        private TextView textViewMemberName;
        private TextView textViewMemberRole;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewMemberIcon = itemView.findViewById(R.id.image_view_member_icon);
            textViewMemberEmail = itemView.findViewById(R.id.text_view_member_email);
            textViewMemberName = itemView.findViewById(R.id.text_view_member_name);
            textViewMemberRole = itemView.findViewById(R.id.text_view_member_role);
        }

        public void bind(User member, String projectOwnerId) {
            if (member != null) {
                // 이메일 설정
                if (member.getEmail() != null && !member.getEmail().isEmpty()) {
                    textViewMemberEmail.setText(member.getEmail());
                    textViewMemberEmail.setVisibility(View.VISIBLE);
                } else {
                    textViewMemberEmail.setVisibility(View.GONE);
                }

                // 표시 이름 설정
                if (member.getDisplayName() != null && !member.getDisplayName().isEmpty()) {
                    textViewMemberName.setText(member.getDisplayName());
                    textViewMemberName.setVisibility(View.VISIBLE);
                } else {
                    textViewMemberName.setVisibility(View.GONE);
                }

                // 이메일과 이름이 모두 없는 경우 기본 텍스트 표시
                if ((member.getEmail() == null || member.getEmail().isEmpty()) &&
                        (member.getDisplayName() == null || member.getDisplayName().isEmpty())) {
                    textViewMemberEmail.setText("알 수 없는 사용자");
                    textViewMemberEmail.setVisibility(View.VISIBLE);
                }

                // 역할 설정
                if (member.getUid() != null && member.getUid().equals(projectOwnerId)) {
                    textViewMemberRole.setText("관리자");
                    textViewMemberRole.setBackgroundColor(0xFFFF6B35); // 주황색
                } else {
                    textViewMemberRole.setText("멤버");
                    textViewMemberRole.setBackgroundColor(0xFF757575); // 회색
                }
            }
        }
    }
}