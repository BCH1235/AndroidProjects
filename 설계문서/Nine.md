# 9. 데이터 뷰

본 시스템은 두 가지 주요 데이터 저장소를 사용한다: 로컬**Room 데이터베이스**와 원격 동기화 및 협업을 위한 **Firebase Firestore**.

## 9.1 로컬 데이터 모델 (Room)
사용자 기기 내에 데이터를 영구적으로 저장하여 오프라인 접근을 지원한다.

*   **`todo_table` (매핑: `TodoItem.java`)**:
    *   **역할**: 개인 및 동기화된 협업 할 일을 저장하는 핵심 테이블.
    *   **주요 컬럼**: `id`, `title`, `content`, `is_completed`, `due_date`, `category_id`, `location_id`, `is_from_collaboration`, `project_id`, `firebase_task_id` 등.
*   **`category_table` (매핑: `CategoryItem.java`)**:
    *   **역할**: 사용자가 생성한 카테고리 정보를 저장.
    *   **주요 컬럼**: `id`, `name`, `color`.
*   **`location_table` (매핑: `LocationItem.java`)**:
    *   **역할**: 위치 기반 알림에 사용될 위치 정보를 저장.
    *   **주요 컬럼**: `id`, `name`, `latitude`, `longitude`, `radius`.

## 9.2 원격 데이터 모델 (Firebase Firestore)
클라우드에 데이터를 저장하여 사용자 간의 실시간 협업과 데이터 백업을 지원한다.

*   **`users` 컬렉션 (매핑: `User.java`)**:
    *   **역할**: 앱에 가입한 사용자 정보를 저장. 문서 ID는 사용자의 `UID`이다.
    *   **주요 필드**: `uid`, `email`, `displayName`.
*   **`projects` 컬렉션 (매핑: `Project.java`)**:
    *   **역할**: 생성된 협업 프로젝트 정보를 저장.
    *   **주요 필드**: `projectId`, `projectName`, `ownerId`, `memberIds`(배열), `memberRoles`(맵).
*   **`project_tasks` 컬렉션 (매핑: `ProjectTask.java`)**:
    *   **역할**: 각 프로젝트에 속한 공유 할 일 정보를 저장.
    *   **주요 필드**: `taskId`, `projectId`, `title`, `isCompleted`, `assignedTo`, `createdBy`, `dueDate`.
*   **`invitations` 컬렉션 (매핑: `ProjectInvitation.java`)**:
    *   **역할**: 사용자 간의 프로젝트 초대 정보를 저장.
    *   **주요 필드**: `invitationId`, `projectId`, `inviterEmail`, `inviteeEmail`, `status` (PENDING, ACCEPTED, REJECTED).

## 9.3 데이터 흐름 및 동기화
1.  **로컬 작업**: 사용자가 개인 할 일을 추가/수정하면 `TodoRepository`를 통해 Room DB에 직접 저장된다.
2.  **협업 작업 (업로드)**: 사용자가 협업 프로젝트에서 할 일을 수정/완료하면, `TodoRepository`는 Room DB를 업데이트한 후, `CollaborationSyncService`를 통해 변경 사항을 Firebase Firestore에 전송한다.
3.  **협업 작업 (다운로드)**: `CollaborationSyncService`는 Firestore의 `project_tasks` 컬렉션에 대한 실시간 리스너를 등록한다. 원격 데이터가 변경되면, 서비스는 변경된 데이터를 로컬의 `TodoItem`으로 변환하여 Room DB에 삽입하거나 업데이트한다.
4.  **UI 업데이트**: Room DAO가 반환하는 `LiveData`를 ViewModel이 관찰하고 있어, 로컬 DB가 변경되면 UI는 자동으로 최신 상태를 반영한다.
