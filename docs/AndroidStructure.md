# SafeLink Android 화면 구조 초안

> 작성: 김재겸 (2026-07-11) | 기준: Design.md 아키텍처(Kotlin·Compose·MVVM·Hilt) + ScreenFlow.md + DevPriority.md
>
> Sprint 4의 Task 4.6 (NavGraph 초안 구성)의 설계 문서. 코드는 스켈레톤이며 구현 시 구체화한다.

---

## 1. 패키지 구조

```
com.safelink.app
├── MainActivity.kt                  // 단일 Activity, Scaffold + NavHost
├── SafeLinkApplication.kt           // @HiltAndroidApp
│
├── ui/
│   ├── theme/                       // Color.kt, Type.kt, Theme.kt (Task 4.3)
│   ├── components/                  // 공통 컴포넌트 (Task 4.4~4.5)
│   │   ├── RiskBadge.kt             //   위험도 배지 (안전/주의/경고/긴급 색상)
│   │   ├── SafeLinkCard.kt          //   흰색 라운드 카드
│   │   ├── SafeLinkButton.kt        //   Primary/Outlined 버튼
│   │   ├── SafeLinkBottomBar.kt     //   하단 탭
│   │   └── SosFab.kt                //   SOS 플로팅 버튼
│   ├── navigation/
│   │   ├── Screen.kt                //   라우트 정의 (sealed class)
│   │   └── SafeLinkNavGraph.kt      //   NavHost + 전체 라우트 등록 (Task 4.6)
│   └── screens/                     // 화면 = 기능별 패키지 (Screen + ViewModel 동거)
│       ├── splash/      SplashScreen.kt
│       ├── onboarding/  OnboardingScreen.kt
│       ├── lock/        LockScreen.kt, LockViewModel.kt
│       ├── home/        HomeScreen.kt, HomeViewModel.kt
│       ├── diagnosis/   DiagnosisScreen.kt, DiagnosisResultScreen.kt, DiagnosisViewModel.kt
│       ├── detection/   DetectionInputScreen.kt, AnalyzingScreen.kt,
│       │                DetectionResultScreen.kt, DetectionViewModel.kt
│       ├── guide/       ResponseGuideScreen.kt
│       ├── support/     SupportMatchScreen.kt, SupportDetailScreen.kt,
│       │                ApplicationGuideScreen.kt, SupportViewModel.kt
│       ├── emergency/   EmergencyScreen.kt, EmergencyViewModel.kt
│       ├── record/      RecordListScreen.kt, MemoEditScreen.kt, RecordViewModel.kt
│       └── settings/    SettingsScreen.kt, SettingsViewModel.kt
│
├── data/
│   ├── model/                       // RiskLevel(enum), Institution, AnalyzeResult 등
│   ├── local/
│   │   ├── db/                      // AppDatabase, DiagnosisDao, DetectionDao (Task 4.10)
│   │   └── prefs/                   // SecurePrefs (EncryptedSharedPreferences, Task 5.10)
│   ├── remote/                      // AnalyzeApiService(Retrofit), dto/ (Task 6.8)
│   └── repository/                  // DiagnosisRepository, DetectionRepository,
│                                    // InstitutionRepository, RecordRepository
│
└── di/                              // Hilt 모듈: DatabaseModule, NetworkModule, PrefsModule
```

**원칙**
- 단일 Activity + Navigation Compose. 화면당 하나의 `@Composable` Screen 함수.
- 화면 패키지 안에 Screen과 ViewModel을 함께 둔다 (대회 규모에서 layer별 분리보다 추적이 쉬움).
- domain 계층은 생략 — 위험도 산출 등 로직은 ViewModel/Repository에 둔다 (Design.md 계층 그대로).

---

## 2. 라우트 정의 (Screen.kt)

```kotlin
sealed class Screen(val route: String) {
    // 진입
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Lock : Screen("lock")

    // 하단 탭
    data object Home : Screen("home")
    data object RecordList : Screen("records")
    data object SupportMatch : Screen("support")
    data object Settings : Screen("settings")
    // ※ 알림 탭 채택 시: data object Notifications : Screen("notifications")

    // 자가진단 (F-01)
    data object Diagnosis : Screen("diagnosis")
    data object DiagnosisResult : Screen("diagnosis_result")   // 결과는 ViewModel 공유

    // 대화 감지 (F-02, F-03)
    data object DetectionInput : Screen("detection_input")
    data object Analyzing : Screen("analyzing")
    data object DetectionResult : Screen("detection_result")

    // 대응·지원 (F-04, F-05, F-06)
    data object ResponseGuide : Screen("guide/{riskLevel}") {
        fun createRoute(riskLevel: RiskLevel) = "guide/${riskLevel.name}"
    }
    data object SupportDetail : Screen("support/{institutionId}") {
        fun createRoute(id: String) = "support/$id"
    }
    data object ApplicationGuide : Screen("application/{institutionId}") {
        fun createRoute(id: String) = "application/$id"
    }

    // 긴급 (F-07)
    data object Emergency : Screen("emergency")

    // 기록 (F-08)
    data object MemoEdit : Screen("memo/{recordId}") {
        fun createRoute(recordId: String) = "memo/$recordId"
    }
}
```

**인자 전달 규칙**
- 라우트 인자는 **ID·enum 같은 가벼운 값만** (institutionId, riskLevel, recordId).
- 분석 결과 같은 복합 객체는 라우트로 넘기지 않는다 — `DiagnosisViewModel`/`DetectionViewModel`을
  NavGraph 범위(`hiltViewModel` + navGraph backStackEntry 스코프)로 공유해서 입력→진행→결과 화면이 같은 인스턴스를 본다.

---

## 3. NavGraph 골격 (SafeLinkNavGraph.kt)

```kotlin
@Composable
fun SafeLinkNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.Lock.route) { LockScreen(navController) }

        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.RecordList.route) { RecordListScreen(navController) }
        composable(Screen.SupportMatch.route) { SupportMatchScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }

        composable(Screen.Diagnosis.route) { DiagnosisScreen(navController) }
        composable(Screen.DiagnosisResult.route) { DiagnosisResultScreen(navController) }

        composable(Screen.DetectionInput.route) { DetectionInputScreen(navController) }
        composable(Screen.Analyzing.route) { AnalyzingScreen(navController) }
        composable(Screen.DetectionResult.route) { DetectionResultScreen(navController) }

        composable(
            route = Screen.ResponseGuide.route,
            arguments = listOf(navArgument("riskLevel") { type = NavType.StringType })
        ) { entry ->
            val level = RiskLevel.valueOf(entry.arguments!!.getString("riskLevel")!!)
            ResponseGuideScreen(navController, level)
        }

        composable(
            route = Screen.SupportDetail.route,
            arguments = listOf(navArgument("institutionId") { type = NavType.StringType })
        ) { entry ->
            SupportDetailScreen(navController, entry.arguments!!.getString("institutionId")!!)
        }

        composable(
            route = Screen.ApplicationGuide.route,
            arguments = listOf(navArgument("institutionId") { type = NavType.StringType })
        ) { entry ->
            ApplicationGuideScreen(navController, entry.arguments!!.getString("institutionId")!!)
        }

        composable(Screen.Emergency.route) { EmergencyScreen(navController) }

        composable(
            route = Screen.MemoEdit.route,
            arguments = listOf(navArgument("recordId") { type = NavType.StringType })
        ) { entry ->
            MemoEditScreen(navController, entry.arguments!!.getString("recordId")!!)
        }
    }
}
```

---

## 4. Scaffold 구성 (MainActivity)

```kotlin
val tabScreens = listOf(Screen.Home, Screen.RecordList, Screen.SupportMatch, Screen.Settings)
// ※ 탭 구성(3탭 vs 5탭)은 팀 확정 후 이 리스트만 수정 (ScreenFlow.md Open Question #1)

val noChromeRoutes = setOf(           // 하단 탭·SOS 버튼을 숨기는 화면
    Screen.Splash.route, Screen.Onboarding.route, Screen.Lock.route,
    Screen.Diagnosis.route, Screen.Emergency.route, Screen.MemoEdit.route,
)

@Composable
fun SafeLinkApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState()
        .value?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute !in noChromeRoutes) {
                SafeLinkBottomBar(navController, tabScreens)
            }
        },
        floatingActionButton = {
            if (currentRoute !in noChromeRoutes) {
                SosFab { navController.navigate(Screen.Emergency.route) }
            }
        }
    ) { padding ->
        SafeLinkNavGraph(navController, Modifier.padding(padding))
    }
}
```

---

## 5. 주요 이동 규칙 (ScreenFlow.md 대응)

| 출발 → 도착 | 호출 방식 | 비고 |
|---|---|---|
| 스플래시 → 홈/온보딩/잠금 | `popUpTo(splash) { inclusive = true }` | 뒤로가기로 스플래시 복귀 방지 |
| 잠금 → 홈 | `popUpTo(lock) { inclusive = true }` | 인증 후 잠금 화면 제거 |
| 진단 결과 → 가이드 | `ResponseGuide.createRoute(riskLevel)` | 위험도 인자 전달 |
| 분석 입력 → 진행 → 결과 | 공유 DetectionViewModel | 결과 객체는 라우트로 안 넘김 |
| 알림(주의/경고) → 가이드 | PendingIntent + DeepLink | Task 6.15 |
| 알림(긴급) → 긴급 화면 | PendingIntent + DeepLink | Task 6.15 |
| 포그라운드 복귀 → 잠금 | MainActivity `onStart`에서 잠금 설정 확인 후 navigate | Task 5.14 |
| 기록 → 분석 결과(재열람) | recordId로 DB 조회 후 DetectionResult 재사용 | Open Question #3 |

---

## 6. 화면별 ViewModel·데이터 의존성

| 화면 | ViewModel | Repository | 원격/로컬 |
|---|---|---|---|
| 홈 | HomeViewModel | RecordRepository | Room (최근 기록) |
| 자가진단·결과 | DiagnosisViewModel (공유) | DiagnosisRepository | Room |
| 대화 분석 3종 | DetectionViewModel (공유) | DetectionRepository | Retrofit(/analyze) + Room |
| 대응 가이드 | — (정적 콘텐츠 + riskLevel 인자) | — | 로컬 리소스 |
| 지원 추천·상세·신청 | SupportViewModel | InstitutionRepository | assets/institutions.json |
| 긴급 | EmergencyViewModel | — (SecurePrefs) | EncryptedSharedPreferences |
| 기록·메모 | RecordViewModel | RecordRepository | Room |
| 설정 | SettingsViewModel | — (SecurePrefs) | EncryptedSharedPreferences |
| 잠금 | LockViewModel | — (SecurePrefs) | PIN 해시 비교 |

---

## 7. 미확정 사항 반영 방법

| Open Question | 코드상 대응 |
|---|---|
| 탭 3개 vs 5개 | `tabScreens` 리스트 한 곳만 수정하면 됨. 알림 탭 채택 시 Screen.Notifications 추가 |
| 텍스트 vs OCR | DetectionInputScreen 내부 세그먼트 탭으로 분리되어 있어 한쪽만 먼저 출시 가능. OCR 채택 시 이미지 업로드용 API·멀티파트 전송 추가 필요 |
| 기록 상세 | DetectionResult를 recordId 인자 버전으로 재사용 (별도 화면 안 만듦) |
