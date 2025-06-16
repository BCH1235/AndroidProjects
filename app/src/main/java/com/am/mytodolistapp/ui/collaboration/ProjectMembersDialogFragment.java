package com.am.mytodolistapp.ui.collaboration;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.Project;
import com.am.mytodolistapp.data.firebase.User;

import java.util.ArrayList;
import java.util.List;

public class ProjectMembersDialogFragment extends DialogFragment {

    private static final String ARG_MEMBERS = "members";
    private static final String ARG_PROJECT_NAME = "project_name";
    private static final String ARG_PROJECT = "project";

    private List<User> membersList;
    private String projectName;
    private Project project;
    private RecyclerView recyclerViewMembers;
    private TextView textViewTitle;
    private Button buttonClose;
    private MemberListAdapter memberAdapter;

    public static ProjectMembersDialogFragment newInstance(List<User> members, String projectName, Project project) {
        ProjectMembersDialogFragment fragment = new ProjectMembersDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MEMBERS, (ArrayList<User>) members);
        args.putString(ARG_PROJECT_NAME, projectName);
        args.putSerializable(ARG_PROJECT, project);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            membersList = (List<User>) getArguments().getSerializable(ARG_MEMBERS);
            projectName = getArguments().getString(ARG_PROJECT_NAME);
            project = (Project) getArguments().getSerializable(ARG_PROJECT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_project_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        updateUI();
    }

    private void initViews(View view) {
        textViewTitle = view.findViewById(R.id.text_view_title);
        recyclerViewMembers = view.findViewById(R.id.recycler_view_members);
        buttonClose = view.findViewById(R.id.button_close);
    }

    private void setupRecyclerView() {
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        memberAdapter = new MemberListAdapter();
        recyclerViewMembers.setAdapter(memberAdapter);
    }

    private void setupClickListeners() {
        buttonClose.setOnClickListener(v -> dismiss());
    }

    private void updateUI() {
        if (projectName != null) {
            textViewTitle.setText(projectName + " 멤버 목록");
        } else {
            textViewTitle.setText("멤버 목록");
        }

        if (membersList != null && !membersList.isEmpty()) {
            memberAdapter.setMembers(membersList);

            // 프로젝트 owner 정보 설정
            if (project != null && project.getOwnerId() != null) {
                memberAdapter.setProjectOwnerId(project.getOwnerId());
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // 다이얼로그 크기 설정
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}