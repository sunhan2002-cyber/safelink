package com.safelink.app.settings

import android.content.Context

/**
 * 온보딩 화면을 최초 1회만 보여주기 위한 완료 여부 저장.
 *
 * [com.safelink.app.security.AppLockManager]와 같은 SharedPreferences 패턴을 그대로 따른다 —
 * 새 저장 방식을 도입하지 않고 기존 컨벤션 재사용.
 */
object OnboardingManager {

    private const val PREFS = "safelink_onboarding"
    private const val KEY_COMPLETED = "onboarding_completed"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 온보딩을 이미 봤는지(=시작하기를 눌러 완료했는지). */
    fun hasCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMPLETED, false)

    /** 온보딩 완료 표시 — "시작하기" 버튼을 누른 시점에 호출한다. */
    fun markCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_COMPLETED, true).apply()
    }
}
