package com.safelink.app.ui.navigation

import com.safelink.app.data.model.RiskLevel

/**
 * 전체 화면 라우트 정의 (docs/AndroidStructure.md 2장)
 * 라우트 인자는 ID·enum 같은 가벼운 값만 전달.
 * 분석 결과 등 복합 객체는 NavGraph 범위 공유 ViewModel로 전달한다.
 */
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

    // 자가진단 (F-01)
    data object Diagnosis : Screen("diagnosis")
    data object DiagnosisResult : Screen("diagnosis_result")

    // 대화 감지 (F-02, F-03)
    data object DetectionInput : Screen("detection_input")
    data object Analyzing : Screen("analyzing")
    data object DetectionResult : Screen("detection_result")

    // 대응·지원 (F-04, F-05, F-06)
    data object ResponseGuide : Screen("guide/{riskLevel}") {
        const val ARG_RISK_LEVEL = "riskLevel"
        fun createRoute(riskLevel: RiskLevel) = "guide/${riskLevel.name}"
    }

    data object SupportDetail : Screen("support_detail/{institutionId}") {
        const val ARG_INSTITUTION_ID = "institutionId"
        fun createRoute(id: String) = "support_detail/$id"
    }

    data object ApplicationGuide : Screen("application/{institutionId}") {
        const val ARG_INSTITUTION_ID = "institutionId"
        fun createRoute(id: String) = "application/$id"
    }

    // 긴급 (F-07)
    data object Emergency : Screen("emergency")

    // 기록 (F-08)
    data object MemoEdit : Screen("memo/{recordId}") {
        const val ARG_RECORD_ID = "recordId"
        fun createRoute(recordId: String) = "memo/$recordId"
    }
}
