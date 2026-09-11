package com.safelink.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.safelink.app.ui.navigation.Screen

private fun tabIcon(screen: Screen): ImageVector = when (screen) {
    Screen.RecordList -> Icons.AutoMirrored.Filled.List
    Screen.SupportMatch -> Icons.Filled.SupportAgent
    Screen.Settings -> Icons.Filled.Settings
    else -> Icons.Filled.Home
}

private fun tabLabel(screen: Screen): String = when (screen) {
    Screen.RecordList -> "기록"
    Screen.SupportMatch -> "지원"
    Screen.Settings -> "설정"
    else -> "홈"
}

@Composable
fun SafeLinkBottomBar(navController: NavHostController, tabs: List<Screen>) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    // SupportMatch는 route 자체가 "support?riskTypes={riskTypes}" 패턴 문자열이라
                    // 그대로 navigate()에 넘기면 안 되고 createRoute()로 실제 목적지를 만들어야 한다
                    // (분석 결과 화면에서 위험유형을 실어 보내는 것과 같은 이유, Task 5.3).
                    val destination =
                        if (screen is Screen.SupportMatch) screen.createRoute() else screen.route
                    navController.navigate(destination) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = tabIcon(screen), contentDescription = tabLabel(screen)) },
                label = { Text(tabLabel(screen)) }
            )
        }
    }
}
