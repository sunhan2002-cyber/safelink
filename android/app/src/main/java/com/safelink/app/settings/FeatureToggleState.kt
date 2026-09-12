package com.safelink.app.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 기능확장 토글의 앱 레벨 상태.
 *
 * 설정 화면의 토글이 화면 로컬 상태에만 머물지 않고 실제 기능 동작을 켜고 끄도록,
 * 여러 화면이 함께 구독하는 단일 소스로 둔다(BackgroundDetectionState 와 같은 패턴).
 *
 * 값은 SharedPreferences 에도 남긴다 — 예전에는 메모리에만 있어서 앱을 껐다 켜면 사용자가 꺼 둔
 * 스크린샷 분석이 다시 켜져 있었다(설정으로서 성립하지 않는 동작).
 */
object FeatureToggleState {

    private const val PREFS = "safelink_settings"
    private const val KEY_SCREENSHOT = "screenshot_analysis_enabled"

    /** 스크린샷 분석 사용 여부 — off 면 대화 분석 입력 화면에서 스크린샷 탭을 숨긴다. */
    private val _screenshotAnalysisEnabled = MutableStateFlow(true)
    val screenshotAnalysisEnabled: StateFlow<Boolean> = _screenshotAnalysisEnabled.asStateFlow()

    /** 저장된 값을 메모리 상태로 올린다. 설정 화면 등 토글을 읽는 화면에서 먼저 부른다. */
    fun load(context: Context) {
        _screenshotAnalysisEnabled.value = prefs(context).getBoolean(KEY_SCREENSHOT, true)
    }

    fun setScreenshotAnalysisEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SCREENSHOT, enabled).apply()
        _screenshotAnalysisEnabled.value = enabled
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
