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

    /** 접근성 설정 화면으로 보낸다 — 켜고 끄는 것은 사용자가 시스템 설정에서 직접 한다. */
    fun openAccessibilitySettings(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }
}
