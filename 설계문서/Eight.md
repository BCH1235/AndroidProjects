# 8. 구현 뷰

## 8.1 개요
본 시스템은 Android Studio IDE를 사용하여 Gradle 빌드 시스템을 기반으로 구현되었다. 모든 소스 코드는 `app` 모듈 내에 구성되어 있다.

## 8.2 소스 코드 및 리소스 구성

*   **Java 소스 코드 (`app/src/main/java/com/am/mytodolistapp`)**:
    *   **`data/`**: 데이터 계층 관련 클래스.
        *   **`firebase/`**: Firebase 데이터 소스 및 모델. (`FirebaseRepository`, `Project`, `User` 등)
        *   **`sync/`**: 로컬-원격 동기화 로직. (`CollaborationSyncService`, `DataSyncUtil`)
        *   `AppDatabase.java`, `TodoRepository.java`, 각 `Dao` 및 `Entity` 클래스.
    *   **`ui/`**: 프레젠테이션 계층 관련 클래스.
        *   **`auth/`**, **`calendar/`**, **`category/`**, **`collaboration/`**, **`location/`**, **`stats/`**, **`task/`**: 기능별로 세분화된 UI(Fragment, Adapter) 및 ViewModel 패키지.
    *   **`service/`**: 백그라운드 서비스 클래스. (`LocationService`)
    *   **`receiver/`**: 브로드캐스트 리시버 클래스. (`GeofenceBroadcastReceiver`)
    *   `MainActivity.java`: 앱의 메인 진입점 및 내비게이션 컨테이너.
    *   `MyTodoApplication.java`: 앱 전역 초기화를 담당하는 Application 클래스.

*   **리소스 파일 (`app/src/main/res`)**:
    *   `layout/`: 각 화면(Fragment, Activity, Dialog) 및 목록 아이템의 UI 구조를 정의하는 XML 파일. (예: `fragment_task_list_improved.xml`, `item_todo_unified.xml`)
    *   `drawable/`: 아이콘, 버튼 배경, 커스텀 도형 등 그래픽 리소스를 담는 XML 또는 이미지 파일.
    *   `menu/`: 옵션 메뉴 및 내비게이션 드로어 메뉴를 정의하는 XML 파일. (예: `drawer_menu.xml`, `task_list_menu.xml`)
    *   `values/`: 앱의 색상(`colors.xml`), 문자열(`strings.xml`), 테마 및 스타일(`themes.xml`)을 정의하는 파일.
    *   `anim/`: UI 애니메이션 효과를 정의하는 XML 파일. (예: `rotate_180.xml`)

*   **빌드 스크립트**:
    *   `build.gradle (Module: app)`: `app` 모듈의 의존성 라이브러리(Room, Firebase, Google Maps 등), SDK 버전, 빌드 구성 등을 정의한다.
    *   `google-services.json`: Firebase 프로젝트와 앱을 연결하기 위한 구성 파일.
