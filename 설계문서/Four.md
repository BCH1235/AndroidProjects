# 4. 유스케이스 뷰

## 4.1 주요 액터
*   **사용자**: 앱의 모든 기능을 사용하는 주체.

## 4.2 유스케이스 목록

### 4.2.1 기본 할 일 관리
*   **할 일 생성**: 사용자는 제목, 내용, 마감 기한, 카테고리를 포함한 새 할 일을 생성한다. (관련: `AddTodoDialogFragment`, `TaskListViewModel`)
*   **할 일 조회**: 사용자는 할 일 목록을 날짜(과거/오늘/미래)별로 그룹화하여 조회한다. (관련: `ImprovedTaskListFragment`, `GroupedTaskAdapter`)
*   **할 일 수정**: 사용자는 기존 할 일의 제목, 기한, 카테고리 등을 수정한다. (관련: `EditTodoDialogFragment`, `TaskListViewModel`)
*   **할 일 완료/취소**: 사용자는 체크박스를 통해 할 일의 완료 상태를 변경한다. (관련: `GroupedTaskAdapter`, `TaskListViewModel`)
*   **할 일 삭제**: 사용자는 할 일을 목록에서 영구적으로 삭제한다. (관련: `GroupedTaskAdapter`, `TaskListViewModel`)
*   **음성으로 할 일 추가**: 사용자는 음성 인식을 통해 빠르게 할 일을 추가한다. (관련: `ImprovedTaskListFragment`)

### 4.2.2 카테고리 관리
*   **카테고리 생성**: 사용자는 이름과 색상을 지정하여 새 카테고리를 만든다. (관련: `AddCategoryDialogFragment`, `CategoryViewModel`)
*   **카테고리 관리**: 사용자는 생성된 카테고리의 목록을 보고 수정하거나 삭제한다. (관련: `CategoryManagementFragment`, `CategoryAdapter`)
*   **카테고리로 필터링**: 사용자는 할 일 목록을 특정 카테고리 기준으로 필터링하여 조회한다. (관련: `ImprovedTaskListFragment`, `CategoryFilterAdapter`)

### 4.2.3 위치 기반 기능
*   **위치 생성 및 관리**: 사용자는 지도에서 위치를 선택하여 '집', '회사' 등의 위치 정보를 생성하고 관리한다. (관련: `LocationBasedTaskFragment`, `MapLocationPickerDialogFragment`)
*   **위치별 할 일 관리**: 사용자는 특정 위치에 종속된 할 일을 추가하고 조회한다. (관련: `LocationTaskListFragment`, `AddLocationTaskDialogFragment`)
*   **위치 기반 알림 수신**: 사용자가 등록된 위치의 지오펜스(Geofence) 영역에 진입하면, 해당 위치와 관련된 미완료 할 일에 대한 알림을 받는다. (관련: `LocationService`, `GeofenceBroadcastReceiver`)

### 4.2.4 협업 기능
*   **사용자 인증**: 사용자는 이메일/비밀번호 또는 Google 계정으로 앱에 로그인하거나 회원가입한다. (관련: `AuthFragment`, `FirebaseRepository`)
*   **프로젝트 생성**: 사용자는 협업을 위한 새 프로젝트를 생성한다. (관련: `CreateProjectDialogFragment`, `CollaborationViewModel`)
*   **멤버 초대**: 사용자는 다른 사용자를 이메일로 프로젝트에 초대한다. (관련: `InviteMemberDialogFragment`, `CollaborationViewModel`)
*   **초대 응답**: 사용자는 받은 프로젝트 초대를 수락하거나 거절한다. (관련: `InvitationListAdapter`, `CollaborationViewModel`)
*   **공유 할 일 관리**: 사용자는 프로젝트 내에서 할 일을 생성, 수정, 삭제하며, 변경 사항은 모든 멤버에게 실시간으로 동기화된다. (관련: `ProjectTaskListFragment`, `ProjectTaskAdapter`)
*   **멤버 목록 확인**: 사용자는 프로젝트에 참여 중인 멤버 목록을 확인한다. (관련: `ProjectMembersDialogFragment`)

### 4.2.5 분석 및 통계
*   **캘린더 조회**: 사용자는 캘린더에서 날짜별 할 일 완료율을 시각적으로 확인하고, 특정 날짜의 할 일 목록을 조회한다. (관련: `ImprovedCalendarFragment`, `CalendarAdapter`)
*   **통계 확인**: 사용자는 전체 할 일의 완료/미완료 개수, 주간 완료 추이(막대그래프), 카테고리별 미완료 작업 분포(파이 차트)를 확인한다. (관련: `StatisticsFragment`, `StatisticsViewModel`)
