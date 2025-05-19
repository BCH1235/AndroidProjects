## 5. 논리 뷰 (Logical View)

시스템 내부가 어떻게 조직되어 있는지를 명확히 하고자 합니다.

### 5.1 개요 (Overview)
본 시스템은 사용자 인터페이스(UI), UI 로직 및 상태 관리, 데이터 처리 및 영속성 관리 등의 주요 관심사를 분리하기 위해 MVVM(Model-View-ViewModel) 아키텍처 패턴을 기반으로 설계되었습니다.

### 5.2 아키텍처적으로 중요한 설계 패키지 (Architecturally Significant Design Packages)

시스템은 다음과 같은 주요 패키지 및 그 하위의 중요한 클래스들로 구성됩니다.

* **`com.am.mytodolistapp.data` (데이터 계층 패키지)**
    * **역할**: 앱의 모든 데이터 처리, 저장, 관리를 담당. 데이터베이스 상호작용 및 데이터 접근 로직을 포함합니다.
    * **주요 클래스 및 설명**:
        * `TodoItem.java` (Entity): 앱에서 관리하는 할 일의 데이터 구조를 정의합니다. (ID, 제목, 내용, 완료 여부, 예상/실제 시간, 완료 시각 등의 속성 포함)
        * `TodoDao.java` (Data Access Object): Room 데이터베이스의 `todo_table`에 접근하여 CRUD(생성, 읽기, 수정, 삭제) 연산을 수행하는 메소드를 정의한 인터페이스입니다. `LiveData`를 사용하여 관찰 가능한 데이터를 반환합니다.
        * `AppDatabase.java` (Room Database): Room 데이터베이스의 전체적인 설정을 담당하며, `TodoItem` 엔티티와 `TodoDao`를 포함하는 데이터베이스 인스턴스를 싱글톤(객체를 딱 하나만 만드는 방식)으로 제공합니다. 데이터베이스 버전 관리 및 마이그레이션 로직을 포함합니다.
        * `TodoRepository.java` (Repository): ViewModel과 데이터 소스(현재는 Room DB) 사이의 데이터 흐름을 관리하고 추상화합니다. 필요한 데이터를 `TodoDao`를 통해 가져오거나 저장하며, 백그라운드 스레드에서 데이터 작업을 실행합니다.

* **`com.am.mytodolistapp.ui` (사용자 인터페이스 계층 패키지)**
    * **역할**: 사용자에게 보여지는 화면을 구성하고, 사용자로부터 입력을 받아 처리하며, ViewModel과 상호작용하여 UI 상태를 업데이트합니다.
    * **주요 클래스 및 설명**:
        * `TaskListFragment.java`: 앱의 메인 화면으로, 할 일 목록을 `RecyclerView`를 통해 사용자에게 보여줍니다. 할 일 추가(FAB, 음성 입력), 수정, 삭제(스와이프) 등의 사용자 인터랙션을 처리하고, `TaskListViewModel`과 데이터를 주고받습니다.
        * `AnalysisFragment.java`: 완료된 할 일에 대한 통계 및 분석 정보를 캘린더(`MaterialCalendarView`)와 함께 시각적으로 제공합니다. `AnalysisViewModel`로부터 분석 데이터를 받아 UI에 표시합니다.
        * `TaskListViewModel.java`: `TaskListFragment`의 UI 상태와 관련된 데이터를 관리합니다. `TodoRepository`를 통해 할 일 데이터를 가져오거나 변경하고, `LiveData`를 사용하여 UI에 데이터 변경을 알립니다.
        * `AnalysisViewModel.java`: `AnalysisFragment`에 필요한 분석 데이터를 가공하고 제공합니다. `TodoRepository`로부터 특정 기간 또는 날짜의 완료된 할 일 데이터를 가져와 통계를 계산하고, `LiveData`로 UI에 전달합니다.
        * `TaskListAdapter.java`: `RecyclerView`에 `TodoItem` 목록을 바인딩하고, 각 항목의 뷰를 생성하며, 체크박스 선택(완료 처리)이나 항목 클릭(수정) 같은 이벤트를 처리하여 `TaskListViewModel` 또는 관련 `DialogFragment`와 상호작용합니다.
        * `AddTodoDialogFragment.java`, `EditTodoDialogFragment.java`, `ActualTimeInputDialogFragment.java`: 각각 새 할 일 추가, 기존 할 일 수정, 실제 소요 시간 입력과 같은 특정 작업을 위한 대화형 UI를 제공하며, `TaskListViewModel`과 연동하여 데이터를 처리합니다.
        * `EventDecorator.java`: `AnalysisFragment`의 `MaterialCalendarView`에 특정 날짜(할 일이 완료된 날짜)에 시각적 표시(점)를 추가하는 커스텀 데코레이터입니다.

* **`com.am.mytodolistapp` (루트 패키지)**
    * **주요 클래스 및 설명**:
        * `MainActivity.java`: 앱의 주 Activity로, 내비게이션 드로어를 포함하며 `TaskListFragment`와 `AnalysisFragment` 간의 화면 전환을 관리하는 컨테이너 역할을 합니다.
        * `MyTodoApplication.java`: Application 클래스를 상속받아 앱 전역에서 필요한 초기화 작업(예: `ThreeTenABP` 라이브러리 초기화)을 수행합니다.
