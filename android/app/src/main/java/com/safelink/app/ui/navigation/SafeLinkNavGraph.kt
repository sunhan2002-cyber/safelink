package com.safelink.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.screens.detection.AnalyzingScreen
import com.safelink.app.ui.screens.detection.DetectionInputScreen
import com.safelink.app.ui.screens.detection.DetectionResultScreen
import com.safelink.app.ui.screens.detection.DetectionViewModel
import com.safelink.app.ui.screens.diagnosis.DiagnosisResultScreen
import com.safelink.app.ui.screens.diagnosis.DiagnosisScreen
import com.safelink.app.ui.screens.emergency.EmergencyScreen
import com.safelink.app.ui.screens.guide.ResponseGuideScreen
import com.safelink.app.ui.screens.home.HomeScreen
import com.safelink.app.ui.screens.lock.LockScreen
import com.safelink.app.ui.screens.onboarding.OnboardingScreen
import com.safelink.app.ui.screens.record.MemoEditScreen
import com.safelink.app.ui.screens.settings.FeatureGuideScreen
import com.safelink.app.ui.screens.record.RecordListScreen
import com.safelink.app.ui.screens.settings.SettingsScreen
import com.safelink.app.ui.screens.splash.SplashScreen
import com.safelink.app.ui.screens.support.ApplicationGuideScreen
import com.safelink.app.ui.screens.support.SupportDetailScreen
import com.safelink.app.ui.screens.support.SupportMatchScreen

/** 전체 화면 라우트 등록 (Task 4.6) */
@Composable
fun SafeLinkNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // 대화 분석 공유 ViewModel — 입력→진행→결과 세 화면이 같은 인스턴스를 본다
    // (Activity 스코프. 실제 데이터 흐름 기준: 김선한_02 문서 4장)
    val detectionViewModel: DetectionViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.Lock.route) { LockScreen(navController) }

        composable(Screen.Home.route) { HomeScreen(navController, detectionViewModel) }
        composable(Screen.RecordList.route) { RecordListScreen(navController) }
        composable(Screen.SupportMatch.route) { SupportMatchScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.FeatureGuide.route) { FeatureGuideScreen(navController) }

        composable(Screen.Diagnosis.route) { DiagnosisScreen(navController) }
        composable(Screen.DiagnosisResult.route) { DiagnosisResultScreen(navController) }

        composable(Screen.DetectionInput.route) {
            DetectionInputScreen(navController, detectionViewModel)
        }
        composable(Screen.Analyzing.route) {
            AnalyzingScreen(navController, detectionViewModel)
        }
        composable(Screen.DetectionResult.route) {
            DetectionResultScreen(navController, detectionViewModel)
        }

        composable(
            route = Screen.ResponseGuide.route,
            arguments = listOf(navArgument(Screen.ResponseGuide.ARG_RISK_LEVEL) {
                type = NavType.StringType
            })
        ) { entry ->
            val levelName = entry.arguments?.getString(Screen.ResponseGuide.ARG_RISK_LEVEL)
            val level = levelName?.let { runCatching { RiskLevel.valueOf(it) }.getOrNull() }
                ?: RiskLevel.CAUTION
            ResponseGuideScreen(navController, level)
        }

        composable(
            route = Screen.SupportDetail.route,
            arguments = listOf(navArgument(Screen.SupportDetail.ARG_INSTITUTION_ID) {
                type = NavType.StringType
            })
        ) { entry ->
            val id = entry.arguments?.getString(Screen.SupportDetail.ARG_INSTITUTION_ID).orEmpty()
            SupportDetailScreen(navController, id)
        }

        composable(
            route = Screen.ApplicationGuide.route,
            arguments = listOf(navArgument(Screen.ApplicationGuide.ARG_INSTITUTION_ID) {
                type = NavType.StringType
            })
        ) { entry ->
            val id = entry.arguments?.getString(Screen.ApplicationGuide.ARG_INSTITUTION_ID).orEmpty()
            ApplicationGuideScreen(navController, id)
        }

        composable(Screen.Emergency.route) { EmergencyScreen(navController) }

        composable(
            route = Screen.MemoEdit.route,
            arguments = listOf(navArgument(Screen.MemoEdit.ARG_RECORD_ID) {
                type = NavType.StringType
            })
        ) { entry ->
            val recordId = entry.arguments?.getString(Screen.MemoEdit.ARG_RECORD_ID).orEmpty()
            MemoEditScreen(navController, recordId)
        }
    }
}
