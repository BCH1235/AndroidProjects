## 3. 아키텍처 목표 및 제약사항

### 3.1 아키텍처 목표

* **높은 사용성 및 효율성**:
    * 사용자가 앱의 기능을 직관적으로 이해하고 쉽게 사용할 수 있도록 합니다.
    * 할 일 추가, 완료 처리 등의 주요 작업을 최소한의 노력으로 빠르게 수행할 수 있도록 지원합니다. (음성 입력, 간편 체크 기능 등)
    * 시간 관리 및 분석 기능을 통해 사용자가 자신의 작업 패턴을 파악하고 개선하는 데 도움을 줍니다.

* **유지보수 용이성**:
    * 코드의 구조를 명확하고 이해하기 쉽게 설계하여, 향후 버그 수정이나 기능 개선이 용이하도록 합니다. (MVVM 패턴, 모듈화된 코드 구조 활용)

* **확장성**:
    * 향후 새로운 기능(예: 회의록의 A, B 그룹 기능)을 추가하거나 기존 기능을 확장할 때, 현재 아키텍처에 큰 변경 없이 유연하게 대응할 수 있도록 설계합니다.

### 3.2 제약 사항

* **플랫폼 제약**: 본 시스템은 안드로이드 모바일 운영체제에서 실행되는 것을 전제로 합니다.
* **기술 스택 제약**:
    * 주 개발 언어: Java
    * 데이터베이스: Android Room Persistence Library 사용
    * 아키텍처 패턴: MVVM (Model-View-ViewModel) 패턴 적용
* **주요 라이브러리**:앱 개발에 활용한 핵심 라이브러리들입니다.
    * **Android Jetpack (LiveData, ViewModel)**: 데이터 변경을 감지하고 UI를 안정적으로 관리하기 위해 사용
    * **Material Components**: 일관되고 현대적인 UI 디자인을 적용하기 위해 활용
    * **MaterialCalendarView**: 사용자에게 직관적인 캘린더 인터페이스를 제공하고, 날짜 선택 및 이벤트 표시 기능을 구현하는 데 사용
    * **ThreeTenABP**: 안드로이드에서 정확하고 편리한 날짜 및 시간 처리를 위해 사용
* **개발 환경**: Android Studio
* **프로젝트 제약**:2025_06_03일 까지 개발을 완료해야 합니다.
