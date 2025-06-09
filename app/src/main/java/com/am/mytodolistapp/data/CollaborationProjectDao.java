package com.am.mytodolistapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CollaborationProjectDao {

    @Insert
    void insert(CollaborationProject project);

    @Update
    void update(CollaborationProject project);

    @Delete
    void delete(CollaborationProject project);

    // 사용자가 참여한 모든 프로젝트 조회
    @Query("SELECT DISTINCT p.* FROM collaboration_project_table p " +
            "INNER JOIN project_member_table m ON p.project_id = m.project_id " +
            "WHERE m.user_id = :userId AND m.is_active = 1 AND m.invitation_status = 'accepted' " +
            "ORDER BY p.updated_at DESC")
    LiveData<List<CollaborationProject>> getUserProjects(String userId);

    // 프로젝트 ID로 조회
    @Query("SELECT * FROM collaboration_project_table WHERE project_id = :projectId")
    LiveData<CollaborationProject> getProjectById(String projectId);

    @Query("SELECT * FROM collaboration_project_table WHERE project_id = :projectId")
    CollaborationProject getProjectByIdSync(String projectId);

    // 사용자가 소유한 프로젝트 조회
    @Query("SELECT * FROM collaboration_project_table WHERE owner_id = :ownerId ORDER BY created_at DESC")
    LiveData<List<CollaborationProject>> getProjectsByOwner(String ownerId);

    // 프로젝트 이름으로 검색
    @Query("SELECT DISTINCT p.* FROM collaboration_project_table p " +
            "INNER JOIN project_member_table m ON p.project_id = m.project_id " +
            "WHERE m.user_id = :userId AND m.is_active = 1 AND m.invitation_status = 'accepted' " +
            "AND p.name LIKE '%' || :searchQuery || '%' " +
            "ORDER BY p.updated_at DESC")
    LiveData<List<CollaborationProject>> searchUserProjects(String userId, String searchQuery);

    @Query("DELETE FROM collaboration_project_table")
    void deleteAllProjects();
}