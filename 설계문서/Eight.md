## 8. 구현 뷰 (Implementation View)

"나만의 할 일 관리 앱"을 구성하는 소프트웨어의 실제 구현 모습, 즉 소스 코드의 구성, 주요 모듈 및 빌드 환경 등에 대해 기술합니다. 논리 뷰에서 설명된 설계 요소들이 실제 개발 환경에서 어떻게 물리적으로 구성되고 관리되는지를 보여줍니다.

### 8.1 개요 (Overview)
본 시스템은 주로 Java 프로그래밍 언어를 사용하여 Android Studio 개발 환경에서 구현되었습니다.

### 8.2 소스 코드 및 리소스 구성 (Source Code and Resource Organization)


* **Java 소스 코드 (`app/src/main/java`)**:
    * **`com.am.mytodolistapp` (루트 패키지)**:
        * **주요 파일**: `MainActivity.java`, `MyTodoApplication.java`
        * **설명**: 앱의 최상위 패키지로, 앱의 시작점(`MyTodoApplication`)과 메인 화면 및 내비게이션을 담당하는 `MainActivity`가 위치합니다.
    * **`com.am.mytodolistapp.data` (데이터 레이어)**:
        * **주요 파일**: `AppDatabase.java`, `TodoDao.java`, `TodoItem.java`, `TodoRepository.java`
        * **설명**: 앱의 데이터 영속성 및 데이터 접근 로직을 담당하는 레이어입니다. Room 데이터베이스 설정, Entity 정의, DAO 인터페이스, 그리고 데이터 소스와 상호작용하는 Repository 클래스가 포함되어 있습니다.
    * **`com.am.mytodolistapp.ui` (프레젠테이션 레이어)**:
        * **주요 파일**: `TaskListFragment.java`, `AnalysisFragment.java`, `TaskListViewModel.java`, `AnalysisViewModel.java`, `TaskListAdapter.java`, 각종 `DialogFragment` 등
        * **설명**: 사용자 인터페이스(UI) 및 사용자 상호작용을 처리하는 프레젠테이션 레이어입니다. MVVM 패턴에 따라 Fragment(View), ViewModel, Adapter 등이 이 패키지 내에 구성되어 있으며, 사용자와 직접 소통하는 화면 로직을 담당합니다.

* **리소스 파일 (`app/src/main/res`)**:
    * **주요 하위 디렉터리 및 설명**:
        * `drawable/`: 앱에서 사용되는 이미지 파일(PNG, JPG 등)이나 XML로 정의된 그래픽 리소스(예: 버튼 모양, 배경)가 위치합니다. (예: 아이콘, 배경 이미지)
        * `layout/`: Activity, Fragment, DialogFragment 등 각 화면 구성 요소의 UI 구조를 정의하는 XML 레이아웃 파일들이 위치합니다. (예: `fragment_task_list.xml`, `dialog_add_todo.xml`, `list_item_todo.xml`)
        * `values/`:
            * `strings.xml`: 앱 내에서 사용되는 모든 문자열(텍스트)을 정의하여 다국어 지원 및 유지보수를 용이하게 합니다. (예: 버튼 텍스트, 안내 문구)
            * `colors.xml`: 앱 전체에서 공통으로 사용되는 색상 값들을 정의합니다.
            * `styles.xml` 또는 `themes.xml`: 앱의 전체적인 테마나 특정 UI 요소의 스타일을 정의합니다.

* **빌드 스크립트**:
    * `build.gradle (Module: app)`: `app` 모듈의 의존성 라이브러리(Room, ViewModel, LiveData, MaterialCalendarView, ThreeTenABP 등), SDK 버전, 빌드 구성 등을 정의합니다.
    * `build.gradle (Project)`: 프로젝트 전체의 빌드 관련 설정을 정의합니다.
