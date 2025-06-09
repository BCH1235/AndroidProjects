package com.am.mytodolistapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProjectMemberDao {

    @Insert
    void insert(ProjectMember member);

    @Insert
    void insertAll(List<ProjectMember> members);

    @Update
    void update(ProjectMember member);

    @Delete
    void delete(ProjectMember member);

    // 특정 프로젝트의 모든 멤버 조회
    @Query("SELECT * FROM project_member_table WHERE project_id = :projectId AND is_active = 1 ORDER BY joined_at ASC")
    LiveData<List<ProjectMember>> getProjectMembers(String projectId);

    // 특정 프로젝트의 활성 멤버 조회 (초대 수락한 멤버만)
    @Query("SELECT * FROM project_member_table WHERE project_id = :projectId AND is_active = 1 AND invitation_status = 'accepted' ORDER BY joined_at ASC")
    LiveData<List<ProjectMember>> getActiveProjectMembers(String projectId);

    // 사용자의 프로젝트 멤버십 조회
    @Query("SELECT * FROM project_member_table WHERE user_id = :userId AND is_active = 1")
    LiveData<List<ProjectMember>> getUserMemberships(String userId);

    // 특정 프로젝트에서 특정 사용자의 멤버십 조회
    @Query("SELECT * FROM project_member_table WHERE project_id = :projectId AND user_id = :userId")
    LiveData<ProjectMember> getProjectMember(String projectId, String userId);

    @Query("SELECT * FROM project_member_table WHERE project_id = :projectId AND user_id = :userId")
    ProjectMember getProjectMemberSync(String projectId, String userId);

    // 특정 사용자의 대기 중인 초대 조회
    @Query("SELECT * FROM project_member_table WHERE user_id = :userId AND invitation_status = 'pending' AND is_active = 1 ORDER BY joined_at DESC")
    LiveData<List<ProjectMember>> getPendingInvitations(String userId);

    // 프로젝트의 멤버 수 조회
    @Query("SELECT COUNT(*) FROM project_member_table WHERE project_id = :projectId AND is_active = 1 AND invitation_status = 'accepted'")
    int getProjectMemberCount(String projectId);

    // 이메일로 사용자 검색 (초대용)
    @Query("SELECT * FROM project_member_table WHERE user_email = :email LIMIT 1")
    ProjectMember findMemberByEmail(String email);

    @Query("DELETE FROM project_member_table")
    void deleteAllMembers();
}