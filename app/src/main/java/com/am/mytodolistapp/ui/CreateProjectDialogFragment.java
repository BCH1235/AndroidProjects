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
import com.am.mytodolistapp.data.CollaborationProject;

public class CreateProjectDialogFragment extends DialogFragment {

    private EditText editProjectName;
    private EditText editProjectDescription;
    private Button buttonCancel, buttonCreate;
    private CollaborationViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CollaborationViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_project, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();

        editProjectName.requestFocus();
    }

    private void initViews(View view) {
        editProjectName = view.findViewById(R.id.edit_project_name);
        editProjectDescription = view.findViewById(R.id.edit_project_description);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonCreate = view.findViewById(R.id.button_create);
    }

    private void setupClickListeners() {
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonCreate.setOnClickListener(v -> createProject());
    }

    private void createProject() {
        String name = editProjectName.getText().toString().trim();
        String description = editProjectDescription.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "프로젝트 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 현재 사용자 정보 가져오기 (임시로 더미 데이터 사용)
        String currentUserId = "user123"; // 실제로는 Firebase Auth에서 가져와야 함
        String currentUserName = "사용자"; // 실제로는 Firebase Auth에서 가져와야 함

        CollaborationProject newProject = new CollaborationProject(name, description, currentUserId, currentUserName);

        viewModel.createProject(newProject, new CollaborationViewModel.CreateProjectCallback() {
            @Override
            public void onSuccess(String projectId) {
                Toast.makeText(getContext(), "프로젝트가 생성되었습니다.", Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "프로젝트 생성 실패: " + error, Toast.LENGTH_LONG).show();
            }
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