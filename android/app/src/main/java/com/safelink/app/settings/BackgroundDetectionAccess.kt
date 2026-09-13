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
     * 실시간 보호 상태를 읽고, 꺼져 있으면 백그라운드 AI 보조분석 동의도 함께 푼다. 현재 보호 상태를 돌려준다.
     *
     * ── 왜 같이 끄는가 ──────────────────────────────────────────────────
     * 백그라운드 AI 보조분석은 실시간 보호 위에서만 의미가 있는 기능이다. 보호를 끈 뒤에도 동의가 남아 있으면
     * 설정에는 "AI 켜짐"으로 보이고, 나중에 보호만 다시 켜도 **다시 묻지 않고** AI 전송이 살아난다.
     * 사용자는 "껐다"고 생각하는데 동의는 살아 있는 상태라, 보호를 끄면 동의도 철회한다.
     * 다시 켤 때는 켜기 창에서 동의를 새로 받는다.
     *
     * ── 왜 여기(화면 복귀 시)에서 하는가 ────────────────────────────────
     * 접근성 서비스는 시스템 설정에서만 꺼지고, 앱은 그 순간을 직접 통보받지 못한다. 서비스의 onUnbind 는
     * 앱 업데이트 때도 불려 그때마다 동의가 풀리게 되므로 쓰지 않았다. 대신 저장된 설정값을 기준으로,
     * 홈·설정 화면이 보일 때마다 맞춘다. 보호가 꺼진 동안에는 서비스가 돌지 않아 그 사이 전송될 일은 없다.
     */
    fun syncAiConsent(context: Context): Boolean {
        val enabled = isEnabled(context)
        if (!enabled && AiConsentStore.isEnabled(context)) {
            AiConsentStore.revoke(context)
        }
        return enabled
    }

    /** 접근성 설정 화면으로 보낸다 — 켜고 끄는 것은 사용자가 시스템 설정에서 직접 한다. */
    fun openAccessibilitySettings(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }
}
