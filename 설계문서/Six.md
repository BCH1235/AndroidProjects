# 6. 프로세스 뷰

사용자의 상호작용과 데이터 동기화를 위해 여러 프로세스와 스레드를 활용한다.

## 6.1 주요 프로세스 및 스레드

*   **UI 스레드**:
    *   **역할**: 모든 UI 렌더링과 사용자 입력 이벤트를 처리한다. `Activity`, `Fragment`, `View`와 관련된 모든 작업은 이 스레드에서 실행된다.

*   **백그라운드 스레드**:
    *   **역할**: Room 데이터베이스 접근, 네트워크 통신 등 시간이 오래 걸릴 수 있는 작업을 처리한다.

## 6.2 주요 시나리오별 프로세스 흐름

### 시나리오 1: 로컬 할 일 추가
1.  **[UI 스레드]** 사용자가 `AddTodoDialogFragment`에서 '추가' 버튼을 클릭한다.
2.  **[UI 스레드]** `TaskListViewModel`의 `insert()` 메서드가 호출된다.
3.  **[UI 스레드]** `ViewModel`은 `TodoRepository`의 `insert()` 메서드를 호출한다.
4.  **[백그라운드 스레드]** `Repository`는 `AppDatabase.databaseWriteExecutor`를 사용하여 `TodoDao`의 `insert()` 메서드를 백그라운드에서 실행한다.
5.  **[백그라운드 스레드]** 데이터베이스에 새 할 일이 추가된다.
6.  **[UI 스레드]** `TodoDao`가 반환하는 `LiveData`가 변경 사항을 감지하고, `TaskListFragment`의 `Observer`에게 알린다.
7.  **[UI 스레드]** `Observer`는 `ListAdapter`에 새로운 목록을 제출하여 UI를 자동으로 업데이트한다.

### 시나리오 2: 협업 데이터 실시간 동기화
1.  **[백그라운드 스레드]** 앱 시작 시 또는 로그인 시, `CollaborationSyncService`가 `FirebaseRepository`를 통해 Firestore의 `project_tasks` 컬렉션에 대한 실시간 리스너(`addSnapshotListener`)를 등록한다.
2.  **[원격]** 다른 사용자가 Firestore의 할 일을 변경한다.
3.  **[백그라운드 스레드]** Firebase SDK가 변경 사항을 감지하고, 등록된 리스너 콜백을 실행한다.
4.  **[백그라운드 스레드]** `CollaborationSyncService`는 콜백으로 받은 데이터를 `DataSyncUtil`을 이용해 로컬 `TodoItem` 객체로 변환한다.
5.  **[백그라운드 스레드]** `Service`는 `TodoDao`를 통해 변환된 `TodoItem`을 로컬 Room DB에 삽입하거나 업데이트한다.
6.  **[UI 스레드]** `TodoDao`의 `LiveData`가 변경을 감지하고, `TaskListFragment`의 UI가 자동으로 업데이트된다.

### 시나리오 3: 위치 기반 알림
1.  **[백그라운드 스레드]** `LocationBasedTaskViewModel`이 `LocationService`를 통해 위치 기반 할 일에 대한 지오펜스(Geofence)를 시스템에 등록한다.
2.  **[시스템]** 사용자의 기기가 등록된 지오펜스 영역에 진입하면, Android 시스템이 `GeofenceBroadcastReceiver`를 호출한다.
3.  **[백그라운드 스레드]** `GeofenceBroadcastReceiver`의 `onReceive()` 메서드가 실행된다.
4.  **[백그라운드 스레드]** `Receiver`는 트리거된 지오펜스의 ID(할 일 ID)를 추출하고, `TodoDao`를 통해 해당 할 일 정보를 조회한다.
5.  **[UI 스레드]** `NotificationHelper`를 통해 사용자에게 할 일 알림을 표시한다.
