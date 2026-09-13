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
    /**
     * 지원 기관 목록. 분석 결과 화면에서 "추천 기관 전체 보기"로 들어올 때는 매칭된
     * 위험유형(institutions.json risk_type_priority 키, 콤마 구분)을 실어 우선순위 정렬에
     * 쓰고, 하단 탭에서 바로 들어오면 인자 없이 전체 목록만 보여준다(Task 5.3).
     */
    data object SupportMatch : Screen("support?riskTypes={riskTypes}") {
        const val ARG_RISK_TYPES = "riskTypes"

        /** 하단 탭 등 맥락 없이 진입(전체 목록) */
        fun createRoute() = "support"

        /** 분석 결과에서 매칭된 위험유형을 들고 진입(해당 기관 우선 정렬) */
        fun createRoute(riskTypes: List<String>): String =
            if (riskTypes.isEmpty()) "support" else "support?riskTypes=${riskTypes.joinToString(",")}"
    }
    data object Settings : Screen("settings")
    data object FeatureGuide : Screen("feature_guide")
    data object PrivacyPolicy : Screen("privacy_policy")

    // 자가진단 (F-01)
    data object Diagnosis : Screen("diagnosis")
    data object DiagnosisResult : Screen("diagnosis_result")

    // 대화 감지 (F-02, F-03)
    data object DetectionInput : Screen("detection_input")
    data object Analyzing : Screen("analyzing")

    /**
     * 링크 검사 전용 화면 — 대화 전체가 아니라 "이 링크가 위험한지"만 확인하고 싶을 때 진입.
     * 홈의 "링크 검사" 타일에서 온다(예전엔 DetectionInput으로 보냈으나 별도 화면으로 분리).
     */
    data object LinkCheck : Screen("link_check")
    /**
     * 분석 결과 화면. 방금 분석한 결과는 공유 ViewModel로 전달되므로 인자가 없고,
     * 기록 탭에서 과거 기록을 다시 열 때만 [ARG_RECORD_ID]로 어떤 기록인지 지정한다.
     */
    data object DetectionResult : Screen("detection_result?recordId={recordId}") {
        const val ARG_RECORD_ID = "recordId"

        /** 방금 분석한 결과 보기(기록 id 없음) */
        fun createRoute() = "detection_result"

        /** 기록 탭에서 과거 기록 열기 */
        fun createRoute(recordId: String) = "detection_result?recordId=$recordId"
    }

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
