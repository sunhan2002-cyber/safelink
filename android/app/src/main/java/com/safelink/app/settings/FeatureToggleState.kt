package com.safelink.app.settings

import android.content.Context
import com.safelink.app.share.SharedImageImporter

/**
 * 기능확장 관련 앱 시작 시 정리.
 *
 * 예전에는 설정에 "스크린샷 분석 사용" 스위치가 있어서 끄면 대화 분석의 스크린샷 탭과 공유 목록의 SafeLink 입구가
 * 숨겨졌다. 스크린샷 분석은 항상 쓰는 기능이라 스위치를 없앴다. 예전에 꺼 둔 사용자는 입구가 꺼진 채 남아 있으므로
 * 앱이 시작될 때 다시 켜고, 남은 설정값도 지운다.
 */
object FeatureToggleState {

    private const val PREFS = "safelink_settings"
    private const val LEGACY_KEY_SCREENSHOT = "screenshot_analysis_enabled"

    fun load(context: Context) {
        SharedImageImporter.setShareTargetEnabled(context, true)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(LEGACY_KEY_SCREENSHOT).apply()
    }
}
