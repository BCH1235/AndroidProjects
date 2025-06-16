# 3. 아키텍처 목표 및 제약사항

## 3.1 아키텍처 목표
*   **유지보수성**: MVVM 아키텍처 패턴과 Repository 패턴을 적용하여 각 계층(UI, ViewModel, Data)의 역할을 명확히 분리한다. 이를 통해 코드의 응집도를 높이고 결합도를 낮춰, 기능 수정 및 추가 시 다른 부분에 미치는 영향을 최소화한다.
*   **확장성**: 로컬 데이터(Room)와 원격 데이터(Firebase)를 Repository에서 통합 관리하도록 설계하여, 향후 새로운 데이터 소스가 추가되더라도 유연하게 확장할 수 있다.
*   **안정성 및 신뢰성**:
    *   **오프라인 지원**: 로컬 데이터베이스(Room)를 주 데이터 소스로 활용하여 네트워크 연결이 없는 상황에서도 앱의 핵심 기능이 동작하도록 보장한다.
    *   **실시간 동기화**: Firebase Firestore의 실시간 리스너를 통해 여러 사용자의 데이터 변경 사항을 즉시 반영하여 협업 환경에서의 데이터 일관성을 유지한다.
*   **사용자 경험**:
    *   `LiveData`와 `ListAdapter`를 활용하여 데이터 변경 시 UI가 부드럽게 자동 업데이트되도록 한다.
    *   위치 서비스, 음성 인식 등 네이티브 기능을 통합하여 사용자에게 편리하고 직관적인 경험을 제공한다.

## 3.2 제약 사항
*   **플랫폼**: Android 운영체제
*   **개발 언어**: Java
*   **개발 환경**: Android Studio
*   **핵심 아키텍처 및 라이브러리**:
    *   **UI 아키텍쳐**: MVVM (Model-View-ViewModel)
    *   **Android Jetpack**: ViewModel, LiveData, Room Persistence Library, Navigation Component
    *   **데이터베이스**:
        *   로컬: Room (SQLite)
        *   원격/협업: Google Firebase (Firestore, Authentication)
    *   **위치 서비스**: Google Play Services (Location, Maps, Places)
    *   **UI 컴포넌트**: Material Components for Android
