package com.am.mytodolistapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.CollaborationProject;
import com.am.mytodolistapp.data.CollaborationProjectDao;
import com.am.mytodolistapp.data.CollaborationTodoDao;
import com.am.mytodolistapp.data.CollaborationTodoItem;
import com.am.mytodolistapp.data.ProjectMember;
import com.am.mytodolistapp.data.ProjectMemberDao;
import com.am.mytodolistapp.service.FirebaseCollaborationService;

import java.util.List;
import java.util.UUID;

public class CollaborationViewModel extends AndroidViewModel {

    private CollaborationProjectDao projectDao;
    private ProjectMemberDao memberDao;
    private CollaborationTodoDao todoDao;
    private FirebaseCollaborationService firebaseService;

    // 현재 사용자 정보 (실제로는 Firebase Auth에서 가져와야 함)
    private String currentUserId = "user123";
    private String currentUserName = "현재사용자";
    private String currentUserEmail = "user@example.com";

    // LiveData
    private LiveData<List<CollaborationProject>> userProjects;
    private LiveData<List<ProjectMember>> pendingInvitations;
    private MutableLiveData<String> selectedProjectId = new MutableLiveData<>();

    public CollaborationViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        projectDao = db.collaborationProjectDao();
        memberDao = db.projectMemberDao();
        todoDao = db.collaborationTodoDao();

        firebaseService = new FirebaseCollaborationService();

        userProjects = projectDao.getUserProjects(currentUserId);
        pendingInvitations = memberDao.getPendingInvitations(currentUserId);
    }

    // 프로젝트 관련 메서드
    public LiveData<List<CollaborationProject>> getUserProjects() {
        return userProjects;
    }

    public LiveData<List<ProjectMember>> getPendingInvitations() {
        return pendingInvitations;
    }

    public void createProject(CollaborationProject project, CreateProjectCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Firebase에 프로젝트 생성
                String projectId = UUID.randomUUID().toString();
                project.setProjectId(projectId);

                // 로컬 데이터베이스에 저장
                projectDao.insert(project);

                // 프로젝트 소유자를 멤버로 추가
                ProjectMember ownerMember = new ProjectMember(
                        projectId, currentUserId, currentUserName, currentUserEmail, "owner"
                );
                memberDao.insert(ownerMember);

                // Firebase에 동기화
                firebaseService.createProject(project, ownerMember, new FirebaseCollaborationService.ProjectCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess(projectId);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void inviteMember(String projectId, String memberEmail, InviteMemberCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 이미 멤버인지 확인
                ProjectMember existingMember = memberDao.findMemberByEmail(memberEmail);
                if (existingMember != null && existingMember.getProjectId().equals(projectId)) {
                    callback.onError("이미 프로젝트 멤버입니다.");
                    return;
                }

                // 초대 생성
                String invitedUserId = "user_" + memberEmail.hashCode(); // 실제로는 이메일로 사용자 검색
                ProjectMember invitation = new ProjectMember(
                        projectId, invitedUserId, memberEmail, memberEmail, "member"
                );
                invitation.setInvitationStatus("pending");

                memberDao.insert(invitation);

                // Firebase에 초대 전송
                firebaseService.inviteMember(projectId, invitation, new FirebaseCollaborationService.InviteCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void acceptInvitation(ProjectMember invitation, AcceptInvitationCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                invitation.setInvitationStatus("accepted");
                memberDao.update(invitation);

                // 프로젝트 멤버 수 업데이트
                CollaborationProject project = projectDao.getProjectByIdSync(invitation.getProjectId());
                if (project != null) {
                    project.setMemberCount(memberDao.getProjectMemberCount(invitation.getProjectId()));
                    projectDao.update(project);
                }

                // Firebase에 동기화
                firebaseService.acceptInvitation(invitation, new FirebaseCollaborationService.AcceptCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void declineInvitation(ProjectMember invitation) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            invitation.setInvitationStatus("declined");
            invitation.setActive(false);
            memberDao.update(invitation);

            // Firebase에 동기화
            firebaseService.declineInvitation(invitation);
        });
    }

    // 할일 관련 메서드
    public LiveData<List<CollaborationTodoItem>> getProjectTodos(String projectId) {
        return todoDao.getProjectTodos(projectId);
    }

    public LiveData<List<CollaborationTodoItem>> getProjectIncompleteTodos(String projectId) {
        return todoDao.getProjectIncompleteTodos(projectId);
    }

    public LiveData<List<CollaborationTodoItem>> getAssignedTodos(String userId) {
        return todoDao.getAssignedTodos(userId);
    }

    public void createTodo(CollaborationTodoItem todo, CreateTodoCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String todoId = UUID.randomUUID().toString();
                todo.setTodoId(todoId);
                todo.setCreatedById(currentUserId);
                todo.setCreatedByName(currentUserName);

                todoDao.insert(todo);

                // Firebase에 동기화
                firebaseService.createTodo(todo, new FirebaseCollaborationService.TodoCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess(todoId);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void updateTodo(CollaborationTodoItem todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.update(todo);
            firebaseService.updateTodo(todo);
        });
    }

    public void completeTodo(CollaborationTodoItem todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todo.setCompleted(true);
            todo.setCompletedById(currentUserId);
            todo.setCompletedByName(currentUserName);
            todo.setCompletedAt(System.currentTimeMillis());

            todoDao.update(todo);
            firebaseService.updateTodo(todo);
        });
    }

    public void assignTodo(String todoId, String assigneeId, String assigneeName) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            CollaborationTodoItem todo = todoDao.getTodoByIdSync(todoId);
            if (todo != null) {
                todo.setAssignedToId(assigneeId);
                todo.setAssignedToName(assigneeName);
                todoDao.update(todo);
                firebaseService.updateTodo(todo);
            }
        });
    }

    public void deleteTodo(CollaborationTodoItem todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.delete(todo);
            firebaseService.deleteTodo(todo.getTodoId());
        });
    }

    // 멤버 관련 메서드
    public LiveData<List<ProjectMember>> getProjectMembers(String projectId) {
        return memberDao.getActiveProjectMembers(projectId);
    }

    public LiveData<CollaborationProject> getProjectById(String projectId) {
        return projectDao.getProjectById(projectId);
    }

    // 현재 사용자 정보
    public String getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    // 콜백 인터페이스들
    public interface CreateProjectCallback {
        void onSuccess(String projectId);
        void onError(String error);
    }

    public interface InviteMemberCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface AcceptInvitationCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface CreateTodoCallback {
        void onSuccess(String todoId);
        void onError(String error);
    }
}