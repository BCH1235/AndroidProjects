## 9. 데이터 뷰 (Data View)

"나만의 할 일 관리 앱"에서 사용되는 데이터의 구조, 저장 방식, 그리고 데이터 접근 방법에 대해 상세히 기술합니다.

### 9.1 데이터 모델 (Data Model)

본 시스템의 핵심 데이터 모델은 사용자의 '할 일(To-do)' 항목을 나타내는 `TodoItem` 엔티티입니다.

* **`TodoItem` 엔티티 (`com.am.mytodolistapp.data.TodoItem.java`)**:
    * **설명**: 사용자가 생성하고 관리하는 개별 할 일 항목 하나하나를 표현하는 데이터 클래스입니다. 이 클래스는 Room 라이브러리에 의해 데이터베이스의 테이블과 매핑됩니다.
    * **주요 속성(필드)**:
        * `id` (Integer, Primary Key, Auto-generated): 각 할 일 항목을 고유하게 식별하는 기본 키입니다. 자동으로 증가하는 값을 가집니다.
        * `title` (String): 할 일의 제목을 저장합니다. 사용자에게 표시되는 주요 내용입니다.
        * `content` (String): 할 일에 대한 추가적인 상세 내용을 저장합니다.
        * `isCompleted` (boolean): 할 일의 완료 여부를 나타냅니다. 기본값은 `false`(미완료)입니다.
        * `estimatedTimeMinutes` (Integer): 해당 할 일을 완료하는 데 예상되는 소요 시간을 분 단위로 저장합니다. 사용자가 할 일 생성/수정 시 입력합니다. 기본값은 `0`입니다.
        * `actualTimeMinutes` (Integer): 해당 할 일을 완료하는 데 실제로 소요된 시간을 분 단위로 저장합니다. 사용자가 할 일을 완료 처리할 때 입력합니다. 기본값은 `0`입니다.
        * `completionTimestamp` (long): 할 일이 완료된 시각을 나타내는 타임스탬프 값입니다 (밀리초 단위). 할 일이 완료 처리될 때 시스템이 현재 시각을 자동으로 기록합니다. 기본값은 `0`입니다.

### 9.2 데이터 저장소 (Data Storage)

본 시스템은 사용자의 할 일 데이터를 기기 내에 영구적으로 저장하기 위해 Android Jetpack의 Room Persistence Library를 사용합니다.

* **데이터베이스**: `AppDatabase.java` (`com.am.mytodolistapp.data.AppDatabase`)
    * **설명**: Room 데이터베이스의 메인 클래스로, 데이터베이스 인스턴스를 싱글톤으로 관리하며, 엔티티(`TodoItem`)와 DAO(`TodoDao`)를 정의하고 데이터베이스 버전 관리 및 마이그레이션 설정을 포함합니다.
    * **데이터베이스명**: "todo_database"
    * **현재 버전**: 2

* **테이블 스키마**: `todo_table`
    * **설명**: `TodoItem` 엔티티와 매핑되는 실제 데이터베이스 테이블입니다. `TodoItem`의 각 속성이 이 테이블의 컬럼으로 생성됩니다.
    * **마이그레이션 (Migration)**:
        * `MIGRATION_1_2`: 데이터베이스 버전 1에서 버전 2로 업그레이드되면서 `todo_table`에 다음 컬럼들이 추가.시간 관리 및 분석 기능 구현을 위해 필요함.
            * `estimated_time_minutes` (INTEGER, NOT NULL, DEFAULT 0)
            * `actual_time_minutes` (INTEGER, NOT NULL, DEFAULT 0)
            * `completion_timestamp` (INTEGER, NOT NULL, DEFAULT 0)

### 9.3 데이터 접근 객체 (Data Access Object - DAO)

데이터베이스와의 실제 상호작용은 `TodoDao` 인터페이스를 통해 이루어집니다.

* **`TodoDao.java` (`com.am.mytodolistapp.data.TodoDao`)**:
    * **설명**: `todo_table`에 대한 데이터베이스 접근 메소드(SQL 쿼리)를 정의하는 인터페이스입니다. Room 라이브러리가 컴파일 시점에 이 인터페이스의 구현체를 자동으로 생성합니다.
    * **주요 기능**:
        * 할 일 항목의 삽입(`insert`), 수정(`update`), 삭제(`delete`)와 같은 기본적인 CRUD 연산을 제공합니다.
        * 모든 할 일 목록을 조회(`getAllTodos`)하거나, 특정 ID의 할 일을 조회(`getTodoById`, `getTodoByIdSync`)하는 기능을 제공합니다. `LiveData`를 반환하여 데이터 변경을 UI에 효과적으로 반영할 수 있도록 합니다.
        * 특정 기간에 완료된 할 일 목록을 조회(`getCompletedTodosBetween`)하는 사용자 정의 쿼리를 제공하여, `AnalysisFragment`의 분석 기능에 필요한 데이터를 추출합니다.
