package com.am.mytodolistapp.data.sync;

import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.data.firebase.ProjectTask;

/**
 * Firebase ProjectTask와 Room TodoItem 간의 데이터 동기화를 위한 유틸리티 클래스
 */
public class DataSyncUtil {

    /**
     * ProjectTask를 TodoItem으로 변환
     * @param projectTask Firebase의 ProjectTask
     * @param projectName 프로젝트 이름
     * @return 변환된 TodoItem
     */
    public static TodoItem convertProjectTaskToTodoItem(ProjectTask projectTask, String projectName) {
        if (projectTask == null) {
            return null;
        }

        TodoItem todoItem = new TodoItem();

        // 기본 필드 매핑
        todoItem.setTitle(projectTask.getTitle());
        todoItem.setContent(projectTask.getContent());
        todoItem.setCompleted(projectTask.isCompleted());
        todoItem.setDueDate(projectTask.getDueDate());
        todoItem.setCreatedAt(projectTask.getCreatedAt());
        todoItem.setUpdatedAt(projectTask.getUpdatedAt());

        // 협업 관련 필드 설정
        todoItem.setFromCollaboration(true);
        todoItem.setProjectId(projectTask.getProjectId());
        todoItem.setFirebaseTaskId(projectTask.getTaskId());
        todoItem.setProjectName(projectName);
        todoItem.setAssignedTo(projectTask.getAssignedTo());
        todoItem.setCreatedBy(projectTask.getCreatedBy());
        todoItem.setPriority(projectTask.getPriority());

        return todoItem;
    }

    /**
     * TodoItem을 ProjectTask로 변환 (역변환)
     * @param todoItem Room의 TodoItem
     * @return 변환된 ProjectTask
     */
    public static ProjectTask convertTodoItemToProjectTask(TodoItem todoItem) {
        if (todoItem == null || !todoItem.isFromCollaboration()) {
            return null;
        }

        ProjectTask projectTask = new ProjectTask();

        // 기본 필드 매핑
        projectTask.setTaskId(todoItem.getFirebaseTaskId());
        projectTask.setProjectId(todoItem.getProjectId());
        projectTask.setTitle(todoItem.getTitle());
        projectTask.setContent(todoItem.getContent());
        projectTask.setCompleted(todoItem.isCompleted());
        projectTask.setDueDate(todoItem.getDueDate());
        projectTask.setCreatedAt(todoItem.getCreatedAt());
        projectTask.setUpdatedAt(todoItem.getUpdatedAt());

        // 협업 관련 필드
        projectTask.setAssignedTo(todoItem.getAssignedTo());
        projectTask.setCreatedBy(todoItem.getCreatedBy());
        projectTask.setPriority(todoItem.getPriority());

        return projectTask;
    }

    /**
     * 기존 TodoItem을 ProjectTask 데이터로 업데이트
     * @param existingTodoItem 기존 TodoItem
     * @param updatedProjectTask 업데이트된 ProjectTask
     * @param projectName 프로젝트 이름
     */
    public static void updateTodoItemFromProjectTask(TodoItem existingTodoItem,
                                                     ProjectTask updatedProjectTask,
                                                     String projectName) {
        if (existingTodoItem == null || updatedProjectTask == null) {
            return;
        }

        // 기본 필드 업데이트
        existingTodoItem.setTitle(updatedProjectTask.getTitle());
        existingTodoItem.setContent(updatedProjectTask.getContent());
        existingTodoItem.setCompleted(updatedProjectTask.isCompleted());
        existingTodoItem.setDueDate(updatedProjectTask.getDueDate());
        existingTodoItem.setUpdatedAt(updatedProjectTask.getUpdatedAt());

        // 협업 관련 필드 업데이트
        existingTodoItem.setProjectName(projectName);
        existingTodoItem.setAssignedTo(updatedProjectTask.getAssignedTo());
        existingTodoItem.setPriority(updatedProjectTask.getPriority());
    }

    /**
     * 두 할 일이 동일한지 비교 (동기화 필요 여부 판단)
     * @param todoItem Room의 TodoItem
     * @param projectTask Firebase의 ProjectTask
     * @return 동일하면 true, 다르면 false
     */
    public static boolean isDataSynced(TodoItem todoItem, ProjectTask projectTask) {
        if (todoItem == null || projectTask == null) {
            return false;
        }

        return equals(todoItem.getTitle(), projectTask.getTitle()) &&
                equals(todoItem.getContent(), projectTask.getContent()) &&
                todoItem.isCompleted() == projectTask.isCompleted() &&
                equals(todoItem.getDueDate(), projectTask.getDueDate()) &&
                equals(todoItem.getAssignedTo(), projectTask.getAssignedTo()) &&
                equals(todoItem.getPriority(), projectTask.getPriority());
    }

    /**
     * null을 고려한 객체 비교
     */
    private static boolean equals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    /**
     * 협업 할 일의 표시 제목 생성 (프로젝트 이름 포함)
     * @param todoItem TodoItem
     * @return 프로젝트 정보가 포함된 제목
     */
    public static String getDisplayTitle(TodoItem todoItem) {
        if (todoItem == null) {
            return "";
        }

        if (todoItem.isFromCollaboration() && todoItem.getProjectName() != null) {
            return "[" + todoItem.getProjectName() + "] " + todoItem.getTitle();
        }

        return todoItem.getTitle();
    }

    /**
     * 우선순위 문자열을 한국어로 변환
     */
    public static String getPriorityDisplayText(String priority) {
        if (priority == null) {
            return "보통";
        }

        switch (priority.toUpperCase()) {
            case "HIGH": return "높음";
            case "MEDIUM": return "보통";
            case "LOW": return "낮음";
            default: return "보통";
        }
    }
}