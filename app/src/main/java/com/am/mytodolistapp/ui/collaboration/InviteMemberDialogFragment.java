package com.am.mytodolistapp.ui.collaboration;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.am.mytodolistapp.R;


// 프로젝트에 새로운 멤버를 초대하기 위한 UI를 제공하는 DialogFragment
//  사용자는 초대할 사람의 이메일을 입력한다
public class InviteMemberDialogFragment extends DialogFragment {

    private static final String ARG_PROJECT_ID = "project_id";
    private static final String ARG_PROJECT_NAME = "project_name";

    private EditText editInviteeEmail;
    private TextView textProjectInfo;
    private OnMemberInvitedListener listener;

    private String projectId;
    private String projectName;

    public interface OnMemberInvitedListener {
        void onMemberInvited(String inviteeEmail);
    }

    // InviteMemberDialogFragment의 새 인스턴스를 생성하고,초대할 프로젝트 정보를 Bundle에 담아 전달
    public static InviteMemberDialogFragment newInstance(String projectId, String projectName) {
        InviteMemberDialogFragment fragment = new InviteMemberDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        args.putString(ARG_PROJECT_NAME, projectName);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnMemberInvitedListener(OnMemberInvitedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectId = getArguments().getString(ARG_PROJECT_ID);
            projectName = getArguments().getString(ARG_PROJECT_NAME);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_invite_member, null);

        textProjectInfo = view.findViewById(R.id.text_project_info);
        editInviteeEmail = view.findViewById(R.id.edit_invitee_email);
        Button buttonCancel = view.findViewById(R.id.button_cancel);
        Button buttonInvite = view.findViewById(R.id.button_invite);
        // 다이얼로그에 현재 프로젝트 이름을 표시
        textProjectInfo.setText("프로젝트: " + projectName);

        buttonCancel.setOnClickListener(v -> dismiss());
        buttonInvite.setOnClickListener(v -> inviteMember());

        builder.setView(view);
        return builder.create();
    }

    //사용자가 입력한 이메일로 멤버를 초대하는 로직을 실행
    private void inviteMember() {
        String email = editInviteeEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(getContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "올바른 이메일 형식을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listener != null) {
            listener.onMemberInvited(email);
        }
        dismiss();
    }
}