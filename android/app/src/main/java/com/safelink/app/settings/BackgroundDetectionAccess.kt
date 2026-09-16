package com.safelink.app.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.safelink.app.background.MessageDetectionService

/**
 * 백그라운드 감지(접근성 서비스)가 실제로 켜져 있는지 확인하고, 켜는 화면으로 보낸다.
 *
 * 예전에는 설정 화면 안에만 이 판단이 있어서, 홈 화면은 권한이 꺼져 있어도 "실시간 보호 중"이라고
 * 표시했다. 상태를 한 곳에서 읽도록 모아 두고 홈·설정이 같은 값을 쓴다.
 */
object BackgroundDetectionAccess {

    /** 시스템 설정에서 SafeLink 접근성 서비스가 켜져 있는지. */
    fun isEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val serviceName = ComponentName(context, MessageDetectionService::class.java).flattenToString()
        return enabledServices
            .split(':')
            .any { it.equals(serviceName, ignoreCase = true) }
    }

    /**
     * 실시간 보호 상태를 읽고, 켜져 있던 보호가 꺼졌으면 AI 보조분석 동의도 함께 푼다. 현재 보호 상태를 돌려준다.
     *
     * ── 왜 같이 끄는가 ──────────────────────────────────────────────────
     * 보호와 AI 를 함께 켠 뒤 보호를 끄면 사용자는 "다 껐다"고 생각한다. 동의가 남아 있으면 나중에 보호만 다시 켜도
     * **다시 묻지 않고** AI 전송이 살아난다. 그래서 보호를 끄는 순간 동의도 철회하고, 다시 켤 때 켜기 창에서 새로 받는다.
     *
     * ── 왜 여기(화면 복귀 시)에서 하는가 ────────────────────────────────
     * 접근성 서비스는 시스템 설정에서만 꺼지고, 앱은 그 순간을 직접 통보받지 못한다. 서비스의 onUnbind 는
     * 앱 업데이트 때도 불려 그때마다 동의가 풀리게 되므로 쓰지 않았다. 대신 마지막으로 본 보호 상태를 저장해 두고,
     * 홈·설정 화면이 보일 때마다 비교한다.
     */
    fun syncAiConsent(context: Context): Boolean {
        val enabled = isEnabled(context)
        // AI 보조분석 동의는 직접 분석에도 쓰인다. 보호를 켜지 않은 사용자가 직접 분석용으로 AI 를 켜 둘 수 있어야 해서,
        // "보호가 꺼져 있다"는 이유만으로 지우지 않고 **켜져 있던 보호가 꺼진 순간**에만 지운다.
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wasEnabled = prefs.getBoolean(KEY_LAST_PROTECTION, enabled)
        if (wasEnabled && !enabled && AiConsentStore.isEnabled(context)) {
            AiConsentStore.revoke(context)
        }
        if (!prefs.contains(KEY_LAST_PROTECTION) || wasEnabled != enabled) {
            prefs.edit().putBoolean(KEY_LAST_PROTECTION, enabled).apply()
        }
        return enabled
    }

    private const val PREFS = "safelink_settings"

    /** 마지막으로 확인한 실시간 보호 상태 — 켜짐 → 꺼짐으로 바뀐 순간을 알아내기 위해 둔다 */
    private const val KEY_LAST_PROTECTION = "last_seen_protection_enabled"

    /** 접근성 설정 화면으로 보낸다 — 켜고 끄는 것은 사용자가 시스템 설정에서 직접 한다. */
    fun openAccessibilitySettings(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }
}
