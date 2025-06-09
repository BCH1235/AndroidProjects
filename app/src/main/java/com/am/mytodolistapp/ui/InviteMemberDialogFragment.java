package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;

public class InviteMemberDialogFragment extends DialogFragment {

    private static final String ARG_PROJECT_ID = "project_id";

    private String projectId;
    private EditText editMemberEmail;
    private Button buttonCancel, buttonInvite;
    private CollaborationViewModel viewModel;

    public static InviteMemberDialogFragment newInstance(String projectId) {
        InviteMemberDialogFragment fragment = new InviteMemberDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CollaborationViewModel.class);

        if (getArguments() != null) {
            projectId = getArguments().getString(ARG_PROJECT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_invite_member, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();

        editMemberEmail.requestFocus();
    }

    private void initViews(View view) {
        editMemberEmail = view.findViewById(R.id.edit_member_email);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonInvite = view.findViewById(R.id.button_invite);
    }

    private void setupClickListeners() {
        buttonCancel.setOnClickListener(v -> dismiss());

        buttonInvite.setOnClickListener(v -> {
            String email = editMemberEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(getContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getContext(), "올바른 이메일 형식을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            buttonInvite.setEnabled(false);
            viewModel.inviteMember(projectId, email, new CollaborationViewModel.InviteMemberCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "초대를 전송했습니다.", Toast.LENGTH_SHORT).show();
                    dismiss();
                }

                @Override
                public void onError(String error) {
                    buttonInvite.setEnabled(true);
                    Toast.makeText(getContext(), "초대 실패: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}