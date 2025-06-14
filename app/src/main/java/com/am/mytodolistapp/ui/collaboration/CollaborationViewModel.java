package com.am.mytodolistapp.ui.collaboration;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.am.mytodolistapp.data.firebase.Project;
import com.am.mytodolistapp.data.firebase.ProjectInvitation;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class CollaborationViewModel extends AndroidViewModel {
    private static final String TAG = "CollaborationViewModel";

    private FirebaseRepository firebaseRepository;
    private LiveData<List<Project>> userProjects;
    private LiveData<List<ProjectInvitation>> userInvitations;
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> successMessage = new MutableLiveData<>();

    public CollaborationViewModel(@NonNull Application application) {
        super(application);
        firebaseRepository = FirebaseRepository.getInstance();
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser != null) {
            userProjects = firebaseRepository.getUserProjects(currentUser.getUid());
            userInvitations = firebaseRepository.getUserInvitations(currentUser.getEmail());
        }
    }

    public LiveData<List<Project>> getUserProjects() {
        return userProjects;
    }

    public LiveData<List<ProjectInvitation>> getUserInvitations() {
        return userInvitations;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void createProject(String projectName, String description) {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            errorMessage.setValue("사용자가 로그인되어 있지 않습니다.");
            return;
        }

        Project project = new Project(null, projectName, description, currentUser.getUid());

        firebaseRepository.createProject(project, new FirebaseRepository.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String projectId) {
                successMessage.setValue("프로젝트가 성공적으로 생성되었습니다.");
                Log.d(TAG, "Project created with ID: " + projectId);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("프로젝트 생성에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to create project", e);
            }
        });
    }

    public void sendInvitation(String projectId, String projectName, String inviteeEmail) {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            errorMessage.setValue("사용자가 로그인되어 있지 않습니다.");
            return;
        }

        ProjectInvitation invitation = new ProjectInvitation(
                null, projectId, projectName,
                currentUser.getUid(), currentUser.getEmail(), inviteeEmail
        );

        firebaseRepository.sendProjectInvitation(invitation, new FirebaseRepository.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String invitationId) {
                successMessage.setValue("초대가 성공적으로 전송되었습니다.");
                Log.d(TAG, "Invitation sent with ID: " + invitationId);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("초대 전송에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to send invitation", e);
            }
        });
    }

    public void respondToInvitation(ProjectInvitation invitation, boolean accept) {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            errorMessage.setValue("사용자가 로그인되어 있지 않습니다.");
            return;
        }

        String status = accept ? "ACCEPTED" : "REJECTED";

        firebaseRepository.respondToInvitation(
                invitation.getInvitationId(),
                status,
                invitation.getProjectId(),
                currentUser.getUid(),
                new FirebaseRepository.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        String message = accept ? "초대를 수락했습니다." : "초대를 거절했습니다.";
                        successMessage.setValue(message);
                        Log.d(TAG, "Invitation response: " + status);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.setValue("초대 응답에 실패했습니다: " + e.getMessage());
                        Log.e(TAG, "Failed to respond to invitation", e);
                    }
                }
        );
    }

    // 프로젝트 삭제 메소드 추가
    public void deleteProject(Project project) {
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser == null) {
            errorMessage.setValue("사용자가 로그인되어 있지 않습니다.");
            return;
        }

        // 프로젝트 소유자인지 확인
        if (!currentUser.getUid().equals(project.getOwnerId())) {
            errorMessage.setValue("프로젝트 소유자만 삭제할 수 있습니다.");
            return;
        }

        firebaseRepository.deleteProjectAndTasks(project.getProjectId(), new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                successMessage.setValue("'" + project.getProjectName() + "' 프로젝트가 삭제되었습니다.");
                Log.d(TAG, "Project deleted successfully: " + project.getProjectId());
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("프로젝트 삭제에 실패했습니다: " + e.getMessage());
                Log.e(TAG, "Failed to delete project", e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 메모리 누수 방지를 위해 Firebase 리스너들 정리
        firebaseRepository.removeAllListeners();
    }
}