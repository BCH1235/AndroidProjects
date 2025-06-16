package com.am.mytodolistapp.data.sync;

import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.data.firebase.ProjectTask;

// 데이터 동기화 과정에서 사용되는 유틸리티 메소드
// 이 클래스의 모든 메소드는 static으로 선언되어 객체를 생성하지 않고 바로 사용가능

// Firebase 모델(`ProjectTask`)과 Room 모델(`TodoItem`) 간의 상호 변환을 담당
// 두 모델의 데이터가 동일한지 비교하여 불필요한 DB 업데이트를 방지
// UI에 표시될 텍스트를 일관된 형식으로 가공

/*  CollaborationSyncService: 이 클래스의 메소드를 사용하여 Firebase 데이터를 로컬 DB에 맞게 변환하고, 동기화 여부를 판단
    TodoRepository: 로컬에서 변경된 `TodoItem`을 Firebase로 보내기 위해 `ProjectTask`로 변환할 때 이 클래스를 사용 */
public class DataSyncUtil {

    // Firebase의 `ProjectTask` 객체를 로컬 DB의 `TodoItem` 객체로 변환
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

        return todoItem;
    }

    // 로컬 DB의 `TodoItem` 객체를 Firebase의 `ProjectTask` 객체로 변환
    // 로컬에서 변경된 할 일 정보를 Firebase에 업데이트할 때 사용한다.
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


        return projectTask;
    }

    //기존 로컬 `TodoItem` 객체의 내용을 새로운 `ProjectTask` 데이터로 업데이트
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

    }


    //로컬 `TodoItem`과 Firebase `ProjectTask`의 내용이 동일한지 비교
    public static boolean isDataSynced(TodoItem todoItem, ProjectTask projectTask) {
        if (todoItem == null || projectTask == null) {
            return false;
        }

        return equals(todoItem.getTitle(), projectTask.getTitle()) &&
                equals(todoItem.getContent(), projectTask.getContent()) &&
                todoItem.isCompleted() == projectTask.isCompleted() &&
                equals(todoItem.getDueDate(), projectTask.getDueDate()) &&
                equals(todoItem.getAssignedTo(), projectTask.getAssignedTo());

    }


    private static boolean equals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    } // null을 포함하여 동일한지 비교하는 메소드




    // 협업 할 일의 경우, UI에 표시될 제목 앞에 [프로젝트명]을 붙여준다.
    public static String getDisplayTitle(TodoItem todoItem) {
        if (todoItem == null) {
            return "";
        }

        if (todoItem.isFromCollaboration() && todoItem.getProjectName() != null) {
            return "[" + todoItem.getProjectName() + "] " + todoItem.getTitle();
        }

        return todoItem.getTitle();
    }

}