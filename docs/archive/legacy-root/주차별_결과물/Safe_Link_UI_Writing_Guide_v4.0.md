# Safe Link UI 기술 문서 v4.0

> 기준 소스: `android/`
> 대상 앱: `com.safelink.app` / version `0.1.0`
> 작성 기준: 현재 저장소의 Kotlin·Compose 코드만 반영. 구현되지 않은 동작은 구현 상태 또는 TODO로 명시한다.

## 1. 프로젝트 개요

SafeLink는 Jetpack Compose로 만든 디지털 위험 분석 지원 앱 UI이다. 사용자는 홈에서 대화 분석 또는 자가 진단을 시작하고, 분석 결과·대응 가이드·지원 기관 정보를 확인할 수 있다. 기록, 메모, 보안 설정, 긴급 도움 요청 화면도 제공한다.

현재 앱은 UI 프로토타입 단계이다. 분석 API, 영속 저장소, 실제 전화·브라우저·문자 실행은 연결되지 않았으며, 분석 결과·기관·기록은 코드 내 더미 데이터로 표시된다.

| 항목 | 현재 구현 |
| --- | --- |
| UI | Kotlin + Jetpack Compose + Material 3 |
| 화면 전환 | Navigation Compose `NavHost` |
| DI 진입점 | Hilt (`SafeLinkApplication`, `MainActivity`) |
| 최소 / 대상 SDK | 26 / 35 |
| 테마 | 라이트 테마 고정 `SafeLinkTheme` |
| 데이터 계층 | 위험도·분석 결과 모델 및 더미 데이터만 존재 |
| ViewModel / Repository / DB | 구현 클래스 없음 |

## 2. v3.1 문서와의 비교

v3.1은 홈, 분석 결과, 대응 가이드의 문구와 미구현 상태(오류, Toast, 권한 Dialog, 푸시, 점검)를 주로 정의한 문서다. v4.0은 문구 가이드가 아니라 현재 Android 코드의 실제 구조와 동작을 기술한다.

| 구분 | v4.0 반영 방식 | 근거 |
| --- | --- | --- |
| 유지 가능한 내용 | 위험도 4단계, 분석 결과와 대응 가이드의 목적 | `RiskLevel`, 결과·가이드 화면 |
| 수정한 내용 | 기관 추천을 현재 UI의 단일 정렬 목록으로 기술 | `DetectionResultScreen`은 `recommendedInstitutions`를 `rank` 순으로 표시 |
| 삭제한 내용 | 오류/Toast/Dialog, 권한 요청, 푸시 알림, 점검, 분석 완료 저장 안내 | 해당 UI 또는 상태 처리 코드 없음 |
| 새로 추가한 내용 | 스플래시, 온보딩, 잠금, 자가 진단, 입력/분석 중, 기록·메모, 지원 상세·신청 안내, 설정·긴급 화면 | 실제 `screens/` 및 `navigation/` 구현 |
| 명확히 제한한 내용 | AI 분석·DB 저장·실제 전화/문자/웹 연결은 미구현으로 표시 | 코드의 TODO 및 더미 데이터 사용 |

## 3. UI 구조

앱 진입점은 `MainActivity`이다. `SafeLinkTheme` 안에서 `SafeLinkApp()`을 구성하고, 하나의 `NavHostController`를 `Scaffold`의 본문, 하단 탭, SOS FAB에 공유한다.

```text
MainActivity
└── SafeLinkTheme
    └── SafeLinkApp
        └── Scaffold
            ├── SafeLinkBottomBar (일부 경로에서만 표시)
            ├── SosFab (일부 경로에서만 표시)
            └── SafeLinkNavGraph
                └── 각 Compose Screen
```

하단 바 탭은 홈, 기록, 지원 서비스, 설정 네 개다. 탭 이동은 시작 목적지까지 `popUpTo`하고 상태 저장·복원을 요청하며, 동일 탭 중복 진입은 `launchSingleTop`으로 막는다.

`splash`, `onboarding`, `lock`, `diagnosis`, `emergency`, `memo/{recordId}` 경로에서는 하단 바와 SOS FAB를 표시하지 않는다. 그 외 등록 경로에서는 두 요소가 표시된다. 따라서 분석 입력·분석 중·결과·가이드·지원 상세·신청 안내에도 현재 코드상 하단 바와 SOS FAB가 함께 표시된다.

## 4. 화면 구성 및 역할

| 영역 | 화면 / 경로 | 역할 | 현재 데이터·상태 |
| --- | --- | --- | --- |
| 진입 | `SplashScreen` / `splash` | 1.5초 로딩 후 홈으로 이동 | `LaunchedEffect` 지연만 사용 |
| 진입 | `OnboardingScreen` / `onboarding` | 서비스 기능 소개와 시작 버튼 | 정적 UI |
| 보안 | `LockScreen` / `lock` | 4자리 PIN 키패드 입력 UI | `pin` 로컬 상태; 네 자리 입력 시 검증 없이 홈 이동 |
| 홈 | `HomeScreen` / `home` | 상태 카드, 요약, 주요 기능·지원 서비스 진입 | 요약 값은 모두 0 |
| 자가 진단 | `DiagnosisScreen` / `diagnosis` | 12개 체크리스트 선택 | 선택한 항목 인덱스 로컬 목록 |
| 자가 진단 | `DiagnosisResultScreen` / `diagnosis_result` | 점수·위험도·근거·다음 행동 표시 | `WARNING`, 55점, 근거 3개 고정 |
| 분석 | `DetectionInputScreen` / `detection_input` | 스크린샷/텍스트 모드 선택 및 분석 시작 | 모드·텍스트 로컬 상태, 텍스트 최대 5,000자 |
| 분석 | `AnalyzingScreen` / `analyzing` | 분석 진행 UI 표시 | 65% 고정 표시 후 3초 뒤 결과 이동 |
| 분석 | `DetectionResultScreen` / `detection_result` | 위험도·점수·감지 요소·키워드·추천 기관 표시 | `DetectionResultDummyData.vpCritical` 고정 |
| 대응 | `ResponseGuideScreen` / `guide/{riskLevel}` | 대응 단계, 주의 사항, 지원기관 이동 | 경로의 `riskLevel`; 가이드 본문은 모든 위험도에 공통 |
| 지원 | `SupportMatchScreen` / `support` | 지원 기관 3개 목록 | `dummyInstitutions` |
| 지원 | `SupportDetailScreen` / `support_detail/{institutionId}` | 기관 정보, 신청 절차, 전화/웹 버튼 | 전달 ID로 더미 기관 검색; 실패 시 첫 기관 |
| 지원 | `ApplicationGuideScreen` / `application/{institutionId}` | 신청 단계·필요 서류·완료 체크 | 완료 단계 인덱스 로컬 목록 |
| 긴급 | `EmergencyScreen` / `emergency` | 112·1366 전화 및 등록 연락처 문자 UI | 외부 실행 미연결 |
| 기록 | `RecordListScreen` / `records` | 분석 기록 3건, 결과/메모 진입 | `dummyRecords` |
| 기록 | `MemoEditScreen` / `memo/{recordId}` | 메모 입력 및 저장 버튼 UI | 메모 로컬 문자열; `recordId`는 화면 내용 조회에 미사용 |
| 설정 | `SettingsScreen` / `settings` | 앱 잠금·생체인증 스위치 및 설정 항목 | 두 스위치 로컬 Boolean 상태 |

## 5. Navigation 구조

`SafeLinkNavGraph`의 시작 목적지는 `splash`다. 모든 목적지는 `Screen` sealed class에 정의되어 있고, 동적 값이 필요한 경로는 문자열 인자를 사용한다.

| Screen | 경로 | 인자 | 인자 처리 |
| --- | --- | --- | --- |
| Splash | `splash` | - | - |
| Onboarding | `onboarding` | - | - |
| Lock | `lock` | - | - |
| Home | `home` | - | - |
| RecordList | `records` | - | - |
| SupportMatch | `support` | - | - |
| Settings | `settings` | - | - |
| Diagnosis | `diagnosis` | - | - |
| DiagnosisResult | `diagnosis_result` | - | - |
| DetectionInput | `detection_input` | - | - |
| Analyzing | `analyzing` | - | - |
| DetectionResult | `detection_result` | - | - |
| ResponseGuide | `guide/{riskLevel}` | `riskLevel: String` | `RiskLevel.valueOf`; 실패 시 `CAUTION` |
| SupportDetail | `support_detail/{institutionId}` | `institutionId: String` | 빈 문자열 허용 |
| ApplicationGuide | `application/{institutionId}` | `institutionId: String` | 빈 문자열 허용 |
| Emergency | `emergency` | - | - |
| MemoEdit | `memo/{recordId}` | `recordId: String` | 빈 문자열 허용 |

### 화면 전환 흐름

```text
splash ──(1.5초)──> home

onboarding ──시작하기──> home
lock ──PIN 4자리 입력──> home

home ──대화 분석──> detection_input ──분석 시작──> analyzing
                                          └──(3초)──> detection_result
home ──자가 진단──> diagnosis ──1개 이상 선택/결과 확인──> diagnosis_result

detection_result / diagnosis_result ──대응 가이드──> guide/{riskLevel}
detection_result / diagnosis_result / guide ──지원 기관──> support
support ──기관 카드──> support_detail/{institutionId} ──신청 절차──> application/{institutionId}

records ──상세 보기──> detection_result
records ──메모 작성──> memo/{recordId}

SOS FAB, 긴급 버튼 ──> emergency ──설정 링크──> settings
```

`DiagnosisResultScreen`의 홈 이동과 `ResponseGuideScreen`의 메인 이동은 `home`까지 inclusive `popUpTo`를 사용한다. 뒤로가기 아이콘이 있는 화면은 기본적으로 `popBackStack()`을 호출한다.

## 6. Screen별 실제 동작

### 분석 흐름

`DetectionInputScreen`은 스크린샷 업로드와 텍스트 입력의 세그먼트 탭을 제공한다. 스크린샷 탭은 선택 UI만 그리며 파일 선택 상태를 갖지 않는다. 이 모드에서는 분석 시작 버튼이 항상 활성화된다. 텍스트 모드에서는 공백이 아닌 텍스트가 있어야 버튼이 활성화되고, 5,000자를 넘는 입력은 반영하지 않는다.

`AnalyzingScreen`은 실제 분석 상태를 수신하지 않는다. 고정 65%와 세 단계 상태를 보여 준 뒤 3초 후 `detection_result`로 교체 이동한다.

`DetectionResultScreen`은 현재 긴급(`CRITICAL`) 보이스피싱 더미 결과만 렌더링한다. 화면 내부의 재사용 가능한 `DetectionResultContent`는 안전·주의·경고·긴급 네 Preview 데이터도 지원한다. 안전이며 매칭 키워드가 없는 경우(`isSafeAndEmpty`)에는 안전 카드만 표시하고 하단 행동 버튼을 숨긴다. 그 외에는 다음을 표시한다.

- 위험 알림, 점수, `RiskBadge`
- 매칭 키워드의 중복 제거된 하위 분류 태그
- 각 매칭 문자열과 설명
- `rank` 오름차순 추천 기관
- 대응 가이드·전문가 상담 연결 버튼

### 자가 진단 흐름

`DiagnosisScreen`은 고위험 표시가 포함된 12개 항목을 제공한다. 하나 이상 선택해야 결과 확인 버튼이 활성화된다. 체크 개수는 화면에 표시되지만, 선택 결과의 가중치 계산 및 전달은 아직 없다.

`DiagnosisResultScreen`은 `WARNING`, 55점, 근거 세 항목을 고정으로 표시한다. 긴급 위험도일 때만 긴급 도움 요청 버튼을 노출하도록 분기되어 있으나, 현재 고정값은 경고이므로 이 버튼은 보이지 않는다. 대응 가이드 또는 지원 기관으로 이동할 수 있다.

### 지원·기록·설정 흐름

지원 목록은 112, 118, 132 세 더미 기관을 카드로 표시한다. 기관 카드 전체를 누르면 상세 화면으로 이동한다. 상세 화면의 신청 절차 링크는 선택한 기관 ID를 유지해 신청 안내 화면으로 전달한다. 신청 안내의 네 단계 체크는 화면 재구성 전까지만 유지된다.

기록 목록은 위험도 배지가 달린 더미 기록 세 건을 표시한다. 상세 보기는 어떤 기록을 선택해도 고정 분석 결과 화면으로 이동한다. 메모 작성은 전달받은 ID를 저장하거나 조회하지 않으며, 저장을 누르면 데이터 저장 없이 이전 화면으로 돌아간다.

설정 화면의 앱 잠금과 생체인증 스위치는 Compose `remember` 상태만 변경한다. 다른 링크는 현재 화면 전환이나 저장을 수행하지 않는다.

## 7. 공통 Component

| 컴포넌트 | 역할 |
| --- | --- |
| `SafeLinkBottomBar` | 홈·기록·지원·설정 탭과 선택 상태를 표시 |
| `SosFab` | 긴급 화면으로 이동하는 빨간 SOS FAB |
| `SafeLinkTopBar` | 제목, 뒤로가기/닫기 아이콘, 선택적 action 슬롯 |
| `SafeLinkPrimaryButton` | 전폭 56dp 높이의 주요 버튼 |
| `SafeLinkOutlinedButton` | 전폭 56dp 높이의 보조 외곽선 버튼 |
| `SafeLinkCard` | 16dp 둥근 모서리의 전폭 카드; 선택적 클릭 및 배경색 지원 |
| `RiskBadge` | `SAFE`, `CAUTION`, `WARNING`, `CRITICAL` 위험도 텍스트와 색상 배지 |
| `RiskLevel.color()` / `containerColor()` | 위험도별 전경·배경 색상 매핑 |
| `PlaceholderScreen` | 제목·설명·action 슬롯을 갖는 범용 플레이스홀더. 현재 NavGraph에서 사용하지 않음 |

## 8. ViewModel 및 UI State 관리

### ViewModel 현황

현재 `app/src/main/java`에는 `ViewModel` 클래스가 없다. Hilt 의존성과 `@HiltAndroidApp`, `@AndroidEntryPoint`는 설정되어 있지만, 화면에서 `viewModel()`, `hiltViewModel()`, `StateFlow`, `collectAsState()`를 사용하지 않는다. 따라서 화면 간 분석·진단 결과를 공유하는 상태 컨테이너도 없다.

### 현재 UI 상태

| 위치 | 상태 | 보존 범위 |
| --- | --- | --- |
| `LockScreen` | `pin: String` | Composable 생존 중 |
| `DetectionInputScreen` | `mode: Int`, `text: String` | Composable 생존 중 |
| `DiagnosisScreen` | 선택 항목의 `mutableStateListOf<Int>` | Composable 생존 중 |
| `ApplicationGuideScreen` | 완료 단계의 `mutableStateListOf<Int>` | Composable 생존 중 |
| `MemoEditScreen` | `memo: String` | Composable 생존 중 |
| `SettingsScreen` | `appLock`, `biometric` | Composable 생존 중 |
| `SplashScreen`, `AnalyzingScreen` | `LaunchedEffect` 기반 지연 전환 | 해당 화면 진입 시 실행 |

이 상태들은 모두 `remember`를 사용한다. `rememberSaveable`, 영속 저장소, SavedStateHandle은 현재 사용되지 않으므로 프로세스 재생성이나 해당 Composable 제거 후의 유지가 보장되지 않는다.

## 9. 데이터 모델과 UI 데이터 흐름

### 모델

| 타입 | 역할 |
| --- | --- |
| `RiskLevel` | `SAFE`, `CAUTION`, `WARNING`, `CRITICAL` 및 표시 라벨 |
| `DetectionResult` | 위험도, 점수, 분류, 원문, 키워드, 추천 기관, 조합 규칙 ID |
| `MatchedKeyword` | 키워드 ID, 하위 분류, 매칭 문자열, 원문 위치, 가중치, 설명 |
| `RecommendedInstitutionUi` | 기관 ID, 이름, 연락처, 순위, 추천 사유, 위험 유형, 그룹 |
| `DetectionResultDummyData` | 결과 화면과 Preview에 쓰는 네 가지 더미 결과 |

`DetectionResult.isSafeAndEmpty`는 매칭 키워드가 비어 있는지를 기준으로 안전 빈 상태 UI를 결정한다. `riskLevel`이 SAFE인지도 함께 확인하지 않으므로, 코드상 이 속성은 키워드 목록이 비었는지를 판별하는 getter이다.

### UI 기준 데이터 흐름

```text
사용자 입력 / 화면 탭
        │
        ├── Compose remember 상태
        │     ├── PIN, 텍스트, 체크리스트, 메모, 설정 스위치
        │     └── 화면을 벗어나면 별도 저장 없음
        │
        └── NavController.navigate(route)
              └── 대상 Screen 렌더링

더미 데이터
  ├── DetectionResultDummyData.vpCritical ──> DetectionResultScreen
  ├── dummyInstitutions ──> SupportMatch / SupportDetail / ApplicationGuide
  └── dummyRecords ──> RecordListScreen
```

분석 입력은 현재 `DetectionResult`로 변환되지 않는다. 자가 진단의 체크 결과도 결과 화면으로 전달되지 않는다. `riskLevel`만 대응 가이드 경로 인자로 전달된다.

## 10. 사용자 동선

1. 앱을 실행하면 스플래시가 1.5초 후 홈으로 이동한다.
2. 홈에서 대화 분석, 자가 진단, 지원 서비스, 하단 탭 또는 SOS를 선택한다.
3. 대화 분석은 입력 방식 선택 후 분석 시작 → 3초 분석 중 화면 → 고정 더미 결과 순서로 진행한다.
4. 분석 결과에서는 대응 가이드 또는 지원 서비스로 이동한다.
5. 자가 진단은 체크리스트를 하나 이상 선택한 뒤 결과 화면에서 가이드·지원 서비스로 이어진다.
6. 지원 서비스에서는 기관 상세와 신청 절차를 확인하고, 절차의 완료 상태를 체크할 수 있다.
7. 기록 탭에서는 더미 기록의 결과 화면 또는 메모 작성 화면으로 이동한다.
8. 어느 Chrome 표시 화면에서나 SOS FAB를 눌러 긴급 도움 요청 화면에 진입할 수 있다.

온보딩 및 잠금 화면 경로도 구현되어 있으나, 현재 스플래시는 첫 실행 여부나 잠금 설정을 판별하지 않고 항상 홈으로 이동한다. 따라서 일반 실행 동선에서는 이 두 화면에 자동 진입하지 않는다.

## 11. 프로젝트 구조

```text
android/
├── app/
│   ├── build.gradle.kts                 # 앱 SDK, Compose, Navigation, Hilt 의존성
│   └── src/main/
│       ├── AndroidManifest.xml          # SafeLinkApplication, MainActivity 등록
│       ├── java/com/safelink/app/
│       │   ├── MainActivity.kt           # Compose 앱 진입, Scaffold 구성
│       │   ├── SafeLinkApplication.kt    # Hilt Application
│       │   ├── data/model/
│       │   │   ├── RiskLevel.kt
│       │   │   ├── DetectionResult.kt
│       │   │   └── DetectionResultDummyData.kt
│       │   └── ui/
│       │       ├── components/           # 공통 버튼, 카드, 바, 위험도 배지
│       │       ├── navigation/           # Screen, SafeLinkNavGraph
│       │       ├── screens/
│       │       │   ├── splash/, onboarding/, lock/, home/
│       │       │   ├── diagnosis/, detection/, guide/, emergency/
│       │       │   ├── support/, record/, settings/
│       │       └── theme/                # Color, Theme, Type
│       └── res/
│           ├── drawable/ic_launcher.xml
│           └── values/                   # 앱 이름·테마 리소스
├── gradle/libs.versions.toml             # 버전 카탈로그
├── build.gradle.kts
└── settings.gradle.kts
```

현재 `ui/viewmodel`, `data/repository`, `data/local`, `data/remote`, `util` 패키지는 존재하지 않는다. 문서상 이러한 계층이 구현된 것처럼 기술하지 않는다.

## 12. 주요 클래스 역할

| 클래스 / 파일 | 역할 |
| --- | --- |
| `MainActivity` | 엣지 투 엣지 활성화, 테마 적용, 최상위 Compose 트리 시작 |
| `SafeLinkApp` | `NavController`, 조건부 하단 바·SOS FAB, `Scaffold` 관리 |
| `SafeLinkApplication` | Hilt 애플리케이션 진입점 |
| `Screen` | 모든 route 및 동적 route 생성 함수 정의 |
| `SafeLinkNavGraph` | route와 Screen Composable 등록, 경로 인자 파싱 |
| `RiskLevel` | 위험도 표시·색상 매핑의 기준 enum |
| `DetectionResult` | 분석 결과 화면의 렌더링 입력 모델 |
| `DetectionResultDummyData` | 현재 결과 화면의 고정 데이터 및 Preview 시나리오 |
| `Institution` / `dummyInstitutions` | 지원 화면에서만 쓰는 내부 더미 기관 모델·목록 |
| `RecordItem` / `dummyRecords` | 기록 화면에서만 쓰는 내부 더미 기록 모델·목록 |

## 13. 현재 구현 상태

| 기능 | 상태 | 비고 |
| --- | --- | --- |
| Compose 화면·공통 컴포넌트 | 구현됨 | 18개 Screen Composable 및 공통 UI 존재 |
| Navigation | 구현됨 | 18개 route, 동적 인자 3종 |
| 하단 탭·SOS 진입 | 구현됨 | 조건부 Chrome 적용 |
| 분석 입력 UI | 부분 구현 | 텍스트 입력은 가능, 이미지 선택·클립보드 미연결 |
| 분석 진행·결과 UI | 부분 구현 | 시간 기반 전환과 더미 결과 |
| 자가 진단 UI | 부분 구현 | 체크는 가능, 점수 계산·결과 전달 미연결 |
| 위험도 UI | 구현됨 | 4단계 색상, 배지, 결과 Preview |
| 지원 기관 UI | 부분 구현 | 더미 3건, 실제 전화·웹 미연결 |
| 기록·메모 UI | 부분 구현 | 더미 기록, 메모 저장 미연결 |
| 잠금·설정 UI | 부분 구현 | 로컬 상태만 존재 |
| 긴급 전화·문자 | 미구현 | 버튼 UI만 존재 |
| API·Repository·DB·ViewModel | 미구현 | 관련 구현 클래스 없음 |

## 14. 코드에 존재하는 TODO 기반 향후 구현 항목

아래는 코드에 TODO로 명시된 항목만 정리한 것이다.

| 영역 | TODO |
| --- | --- |
| 진입·잠금 | 첫 실행/잠금 설정에 따른 스플래시 분기, SHA-256 PIN 검증, PIN 오류 횟수·차단, `BiometricPrompt` |
| 분석 입력 | Photo Picker, 썸네일·삭제, `ClipboardManager` |
| 분석·결과 | API 연동 `DetectionViewModel` 상태 전환, `StateFlow<DetectionResult>` 수신, 원문 인덱스 기반 `AnnotatedString` 강조 |
| 자가 진단 | 가중치 합산, 위험도 분류, `DiagnosisViewModel`로 결과 공유 |
| 대응·긴급 | 위험 유형·위험도별 가이드 분기, `ACTION_DIAL`(112/1332/1366 등), 등록 연락처 문자 발송·미등록 안내 |
| 지원 기관 | 실제 기관 데이터 로드, 전화 Intent, 브라우저 Intent |
| 기록·메모 | Room 기록 조회, 위험도 필터, 빈 상태, Room 메모 저장·미저장 이탈 확인 |
| 설정 | PIN 변경, 긴급 연락처·문자 문구·알림 문구 저장, 데이터 삭제 확인, 개인정보 처리방침 |

## 15. 개선 가능한 점

다음은 현재 구조를 기준으로 한 개선 제안이며, 이미 구현된 기능으로 간주하지 않는다.

- 화면별 `remember` 상태를 ViewModel UI state로 옮기고, 분석·진단 결과를 단일 상태 소스로 전달한다.
- 실제 데이터 소스가 도입될 때 `repository`와 로컬/원격 데이터 계층을 분리해 더미 데이터를 교체한다.
- 경로 인자에 사용되는 기관 ID·기록 ID의 조회 실패를 명시적인 오류 또는 빈 상태로 처리한다. 현재 기관 상세는 실패 시 첫 더미 기관을 표시한다.
- 스플래시, 분석 중 화면의 시간 기반 자동 전환을 실제 초기화·요청 상태 및 오류 상태에 연결한다.
- 전화·문자·브라우저 실행 전 Android 권한, Intent 처리 가능 여부, 사용자의 확인 흐름을 설계한다.
- 잠금, 긴급 연락처, 메모처럼 민감한 정보는 영속화 전에 암호화 저장·인증 실패 정책·삭제 정책을 구체화한다.
- 화면 테스트와 Navigation 테스트를 추가해 탭 상태 복원, 동적 경로 인자, 안전 빈 결과 표시를 검증한다.

## 16. 문서 범위 요약

이 문서는 현재 `android/app/src/main`의 구현을 기준으로 한다. UI의 문구가 분석·저장·보호 기능을 설명하더라도, 코드에 API 호출·DB 저장·외부 Intent·권한 처리·ViewModel 상태 연결이 없는 경우 해당 기능은 구현 완료로 보지 않는다.
