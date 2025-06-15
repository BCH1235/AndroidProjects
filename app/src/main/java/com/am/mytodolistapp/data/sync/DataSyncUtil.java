package com.am.mytodolistapp.data.sync;

import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.data.firebase.ProjectTask;


public class DataSyncUtil {

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

    public static boolean isDataSynced(TodoItem todoItem, ProjectTask projectTask) {
        if (todoItem == null || projectTask == null) {
            return false;
        }

        return todoItem.getTitle().equals(projectTask.getTitle()) &&
                equals(todoItem.getContent(), projectTask.getContent()) &&
                todoItem.isCompleted() == projectTask.isCompleted() &&
                equals(todoItem.getDueDate(), projectTask.getDueDate()) &&
                equals(todoItem.getAssignedTo(), projectTask.getAssignedTo()) &&
                equals(todoItem.getPriority(), projectTask.getPriority());
    }

    private static boolean equals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    public static String getDisplayTitle(TodoItem todoItem) {
        if (todoItem == null) {
            return "";
        }

        if (todoItem.isFromCollaboration() && todoItem.getProjectName() != null) {
            return "[" + todoItem.getProjectName() + "] " + todoItem.getTitle();
        }

        return todoItem.getTitle();
    }


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