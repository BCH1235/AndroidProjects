package com.am.mytodolistapp.ui.collaboration;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.am.mytodolistapp.R;


// 새로운 협업 프로젝트를 생성하기 위한 UI를 제공하는 DialogFragment
// 사용자는 프로젝트 이름과 설명을 입력할 수 있다
public class CreateProjectDialogFragment extends DialogFragment {

    private EditText editProjectName;
    private EditText editProjectDescription;
    private OnProjectCreatedListener listener;

    public interface OnProjectCreatedListener {
        void onProjectCreated(String projectName, String description);
    }

    public void setOnProjectCreatedListener(OnProjectCreatedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_project, null);

        editProjectName = view.findViewById(R.id.edit_project_name);
        editProjectDescription = view.findViewById(R.id.edit_project_description);
        Button buttonCancel = view.findViewById(R.id.button_cancel);
        Button buttonCreate = view.findViewById(R.id.button_create);

        buttonCancel.setOnClickListener(v -> dismiss());
        buttonCreate.setOnClickListener(v -> createProject());

        builder.setView(view);
        return builder.create();
    }

    // 사용자가 입력한 정보로 새 프로젝트를 생성하는 로직을 실행
    private void createProject() {
        String projectName = editProjectName.getText().toString().trim();
        String description = editProjectDescription.getText().toString().trim();

        if (projectName.isEmpty()) {
            Toast.makeText(getContext(), "프로젝트 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listener != null) {
            listener.onProjectCreated(projectName, description);
        }
        dismiss();
    }
}