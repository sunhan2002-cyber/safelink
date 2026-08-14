package com.safelink.app.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 기능확장 토글의 앱 레벨 상태.
 *
 * 설정 화면의 토글이 화면 로컬 상태에만 머물지 않고 실제 기능 동작을 켜고 끄도록,
 * 여러 화면이 함께 구독하는 단일 소스로 둔다(BackgroundDetectionState 와 같은 패턴).
 * 세션 메모리에만 유지하며, 영구 저장은 추후 EncryptedSharedPreferences 로 대체한다(Task 5.15).
 */
object FeatureToggleState {

    /** 스크린샷 분석 사용 여부 — off 면 대화 분석 입력 화면에서 스크린샷 탭을 숨긴다. */
    private val _screenshotAnalysisEnabled = MutableStateFlow(true)
    val screenshotAnalysisEnabled: StateFlow<Boolean> = _screenshotAnalysisEnabled.asStateFlow()

    fun setScreenshotAnalysisEnabled(enabled: Boolean) {
        _screenshotAnalysisEnabled.value = enabled
    }
}
