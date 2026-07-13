package com.safelink.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safelink.app.ui.components.SafeLinkBottomBar
import com.safelink.app.ui.components.SosFab
import com.safelink.app.ui.navigation.SafeLinkNavGraph
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.SafeLinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeLinkTheme {
                SafeLinkApp()
            }
        }
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

@Composable
fun SafeLinkApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute != null && currentRoute !in noChromeRoutes

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
