package com.safelink.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safelink.app.ui.components.SafeLinkBottomBar
import com.safelink.app.ui.components.SosFab
import com.safelink.app.security.AppLockManager
import com.safelink.app.ui.navigation.SafeLinkNavGraph
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.SafeLinkTheme
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
// BiometricPrompt(생체인증, Task 5.13)가 FragmentActivity를 요구해 상속을 넓혔다.
// FragmentActivity는 ComponentActivity의 하위 클래스라 기존 Compose/ViewModel 코드는 그대로 동작한다.
class MainActivity : FragmentActivity() {

    // 알림 탭으로 전달된 딥링크 라우트 (감지 알림 → 대응 가이드/긴급 화면)
    private val pendingRoute = mutableStateOf<String?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 결과와 무관하게 진행 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute.value = intent?.getStringExtra(EXTRA_NAV_ROUTE)
        maybeRequestNotificationPermission()
        setContent {
            SafeLinkTheme {
                SafeLinkApp(pendingRoute)
            }
        }
    }

    /** 앱 실행 중 알림을 탭한 경우 (warm start) — 새 인텐트의 라우트로 이동 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_NAV_ROUTE)
    }

    /** Android 13+ 배너 알림 표시를 위한 런타임 권한 요청 (한 번) */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        /** 알림 탭 시 이동할 NavGraph 라우트를 담는 인텐트 extra 키 */
        const val EXTRA_NAV_ROUTE = "nav_route"
    }
}

/** 하단 탭 4개 확정 (7/13) — 알림 이력은 활동 기록으로 흡수, 보안상 알림 센터 화면 없음 */
val tabScreens = listOf(Screen.Home, Screen.RecordList, Screen.SupportMatch, Screen.Settings)

/** 하단 탭·SOS 버튼을 숨기는 화면 */
val noChromeRoutes = setOf(
    Screen.Splash.route,
    Screen.Onboarding.route,
    Screen.Lock.route,
    Screen.Diagnosis.route,
    Screen.Emergency.route,
    Screen.MemoEdit.route,
)

/** 딥링크 이동을 보류하는 진입/잠금 화면 (여기서는 아직 이동하지 않고 대기) */
private val startupRoutes = setOf(
    Screen.Splash.route,
    Screen.Onboarding.route,
    Screen.Lock.route,
)

@Composable
fun SafeLinkApp(pendingRoute: MutableState<String?>) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute != null && currentRoute !in noChromeRoutes

    // 앱 잠금은 **앱을 새로 켤 때만** 묻는다.
    // 잠깐 다른 앱에 갔다 돌아올 때마다 PIN 을 요구하면 정작 급할 때 대화를 확인하지 못한다(사용자 요청).
    // 보통은 스플래시에서 잠금 화면으로 보내지만, 시스템이 프로세스를 정리한 뒤 마지막 화면으로
    // 복원되는 경우에는 스플래시를 거치지 않는다 — 그때를 여기서 잡는다.
    // 조건이 프로세스 메모리 값([AppLockManager.isUnlockedInProcess])이라, 앱을 완전히 종료하면 다시 잠긴다.
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (AppLockManager.isEnabled(context) && !AppLockManager.isUnlockedInProcess) {
            val route = navController.currentBackStackEntry?.destination?.route
            if (route != null && route !in startupRoutes) {
                navController.navigate(Screen.Lock.route) { popUpTo(Screen.Home.route) }
            }
        }
    }

    // 알림 딥링크 처리: 진입/잠금 화면을 지난 뒤(정상 화면에서) 해당 라우트로 이동
    LaunchedEffect(pendingRoute.value, currentRoute) {
        val route = pendingRoute.value ?: return@LaunchedEffect
        if (currentRoute == null || currentRoute in startupRoutes) return@LaunchedEffect
        navController.navigate(route)
        pendingRoute.value = null
    }

    Scaffold(
        bottomBar = {
            if (showChrome) {
                SafeLinkBottomBar(navController = navController, tabs = tabScreens)
            }
        },
        floatingActionButton = {
            if (showChrome) {
                SosFab(onClick = { navController.navigate(Screen.Emergency.route) })
            }
        }
    ) { padding ->
        SafeLinkNavGraph(
            navController = navController,
            modifier = Modifier.padding(padding)
        )
    }
}
