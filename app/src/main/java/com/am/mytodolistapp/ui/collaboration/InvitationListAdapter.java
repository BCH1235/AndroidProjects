package com.am.mytodolistapp.ui.collaboration;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.ProjectInvitation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

// 받은 프로젝트 초대 목록을 RecyclerView에 표시하기 위한 어댑터
public class InvitationListAdapter extends ListAdapter<ProjectInvitation, InvitationListAdapter.InvitationViewHolder> {

    private OnInvitationResponseListener onAcceptListener;
    private OnInvitationResponseListener onRejectListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREAN);

    public interface OnInvitationResponseListener {
        void onResponse(ProjectInvitation invitation);
    }

    public InvitationListAdapter(OnInvitationResponseListener onAcceptListener,
                                 OnInvitationResponseListener onRejectListener) {
        super(DIFF_CALLBACK);
        this.onAcceptListener = onAcceptListener;
        this.onRejectListener = onRejectListener;
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        ProjectInvitation invitation = getItem(position);
        holder.bind(invitation);
    }

    class InvitationViewHolder extends RecyclerView.ViewHolder {
        private TextView textProjectName;
        private TextView textInviterEmail;
        private TextView textCreatedDate;
        private Button buttonAccept;
        private Button buttonReject;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            textProjectName = itemView.findViewById(R.id.text_invitation_project_name);
            textInviterEmail = itemView.findViewById(R.id.text_inviter_email);
            textCreatedDate = itemView.findViewById(R.id.text_invitation_date);
            buttonAccept = itemView.findViewById(R.id.button_accept_invitation);
            buttonReject = itemView.findViewById(R.id.button_reject_invitation);

            // '수락' 버튼 클릭 시, onAcceptListener를 호출
            buttonAccept.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onAcceptListener != null) {
                    onAcceptListener.onResponse(getItem(position));
                }
            });

            // '거절' 버튼 클릭 시, onRejectListener를 호출
            buttonReject.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onRejectListener != null) {
                    onRejectListener.onResponse(getItem(position));
                }
            });
        }

        // ProjectInvitation 데이터를 뷰에 바인딩
        public void bind(ProjectInvitation invitation) {
            textProjectName.setText("프로젝트: " + invitation.getProjectName());
            textInviterEmail.setText("초대자: " + invitation.getInviterEmail());

            Date createdDate = new Date(invitation.getCreatedAt());
            textCreatedDate.setText("초대일: " + dateFormat.format(createdDate));
        }
    }

    private static final DiffUtil.ItemCallback<ProjectInvitation> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ProjectInvitation>() {
                @Override
                public boolean areItemsTheSame(@NonNull ProjectInvitation oldItem, @NonNull ProjectInvitation newItem) {
                    return Objects.equals(oldItem.getInvitationId(), newItem.getInvitationId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ProjectInvitation oldItem, @NonNull ProjectInvitation newItem) {
                    return Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                            oldItem.getCreatedAt() == newItem.getCreatedAt();
                }
            };
}
