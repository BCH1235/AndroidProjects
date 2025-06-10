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

        textProjectInfo.setText("프로젝트: " + projectName);

        buttonCancel.setOnClickListener(v -> dismiss());
        buttonInvite.setOnClickListener(v -> inviteMember());

        builder.setView(view);
        return builder.create();
    }

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