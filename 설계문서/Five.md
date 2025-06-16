# 5. 논리 뷰

## 5.1 개요
본 시스템은 **MVVM(Model-View-ViewModel) 아키텍처**를 기반으로 설계되었다. 이는 UI와 비즈니스 로직을 분리하여 테스트 용이성과 유지보수성을 높인다. 시스템은 크게 세 가지 논리적 계층으로 구성된다.

*   **프레젠테이션 계층**: 사용자에게 정보를 표시하고 사용자 입력을 처리한다. (Activity, Fragment, Adapter)
*   **도메인 계층**: UI에 표시할 상태를 관리하고 비즈니스 로직을 수행한다. (ViewModel)
*   **데이터 계층**: 데이터의 소스(로컬, 원격)와 상호작용하고, 데이터 처리 로직을 캡슐화한다. (Repository, DAO, Data Source)

## 5.2 아키텍처적으로 중요한 설계 패키지

### `com.am.mytodolistapp.ui` (프레젠테이션 계층)
*   **역할**: 화면 표시와 사용자 상호작용을 담당한다. ViewModel로부터 `LiveData`를 구독하여 UI를 업데이트하고, 사용자 이벤트를 ViewModel에 전달한다.
*   **주요 패키지 및 클래스**:
    *   **`task`**: `ImprovedTaskListFragment`, `AddTodoDialogFragment`, `GroupedTaskAdapter` 등 개인 할 일 관리 UI.
    *   **`collaboration`**: `CollaborationFragment`, `ProjectTaskListFragment`, `ProjectTaskAdapter` 등 협업 관련 UI.
    *   **`location`**: `LocationBasedTaskFragment`, `MapLocationPickerDialogFragment` 등 위치 기반 기능 UI.
    *   **`category`**: `CategoryManagementFragment`, `AddCategoryDialogFragment` 등 카테고리 관리 UI.
    *   **`calendar`**: `ImprovedCalendarFragment`, `CalendarAdapter` 등 캘린더 UI.
    *   **`stats`**: `StatisticsFragment`, `CategoryPieChart` 등 통계 UI.
    *   **`auth`**: `AuthFragment` 등 사용자 인증 UI.

### `com.am.mytodolistapp.data` (데이터 계층)
*   **역할**: 앱의 모든 데이터를 관리하고, 비즈니스 로직을 포함하며, 데이터 소스와의 통신을 담당한다.
*   **주요 패키지 및 클래스**:
    *   **`TodoRepository.java`**: 데이터 계층의 진입점. ViewModel은 이 Repository를 통해서만 데이터에 접근한다. 로컬 DB와 원격 DB 사이의 데이터 흐름을 제어하고, `CollaborationSyncService`를 통해 동기화 로직을 발동한다.
    *   **`firebase`**: Firebase와의 통신을 담당하는 패키지.
        *   `FirebaseRepository.java`: Firestore 및 Firebase Authentication 관련 모든 CRUD 작업을 캡슐화한 클래스.
        *   `Project.java`, `User.java`, `ProjectTask.java`, `ProjectInvitation.java`: Firestore의 각 컬렉션에 매핑되는 데이터 모델(POJO) 클래스.
    *   **`sync`**: 로컬 DB와 원격 DB 간의 데이터 동기화 담당하는 패키지.
        *   `CollaborationSyncService.java`: Firebase의 실시간 변경사항을 감지하여 로컬 Room DB에 반영하는 핵심 동기화 서비스.
        *   `DataSyncUtil.java`: `ProjectTask`(Firebase 모델)와 `TodoItem`(Room 모델) 간의 객체 변환을 돕는 유틸리티 클래스.
    *   **`AppDatabase.java`**: Room 데이터베이스의 전체 설정을 담당하는 추상 클래스.
    *   **`TodoDao.java`, `CategoryDao.java`, `LocationDao.java`**: 각 테이블에 대한 CRUD 작업을 정의한 Room DAO 인터페이스.
    *   **`TodoItem.java`, `CategoryItem.java`, `LocationItem.java`**: Room 데이터베이스의 각 테이블에 매핑되는 Entity 클래스.

### `com.am.mytodolistapp.service` & `receiver`
*   **역할**: 백그라운드 작업을 처리한다.
*   **주요 클래스**:
    *   `LocationService.java`: 지오펜스 등록, 해제 등 위치 관련 백그라운드 로직을 처리.
    *   `GeofenceBroadcastReceiver.java`: 시스템으로부터 지오펜스 전환 이벤트를 수신하여 사용자에게 알림을 보내는 `BroadcastReceiver`.
