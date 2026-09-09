package com.safelink.app.security

import android.content.Context
import java.security.MessageDigest

/**
 * 앱 잠금(PIN) 실제 동작 관리.
 *
 * - 설정에서 앱 잠금을 켜면 4자리 PIN 을 저장하고, 앱 실행 시 잠금 화면을 거치게 한다.
 * - PIN 은 평문 대신 SHA-256 해시로 저장한다.
 * - 저장소는 지금은 SharedPreferences 를 쓴다. 실제 배포 시 EncryptedSharedPreferences 로
 *   교체하면 되며(같은 키/인터페이스 유지), 이 클래스만 바꾸면 화면 코드는 그대로 동작한다(Task 5.15).
 */
object AppLockManager {

    private const val PREFS = "safelink_security"
    private const val KEY_ENABLED = "app_lock_enabled"
    private const val KEY_PIN_HASH = "app_lock_pin_hash"
    private const val KEY_FAIL_COUNT = "pin_fail_count"
    private const val KEY_LOCK_UNTIL = "pin_lock_until"
    private const val KEY_BIOMETRIC = "biometric_enabled"

    /** 연속 실패 허용 횟수 — 초과 시 [LOCKOUT_MS] 동안 입력 차단 (Design.md 5.4) */
    const val MAX_FAIL_COUNT = 5

    /** 입력 차단 시간 */
    const val LOCKOUT_MS = 30_000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 앱 잠금이 켜져 있고 PIN 이 설정된 상태인지. */
    fun isEnabled(context: Context): Boolean {
        val p = prefs(context)
        return p.getBoolean(KEY_ENABLED, false) && !p.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    /** PIN 설정 + 잠금 활성화. */
    fun setPin(context: Context, pin: String) {
        prefs(context).edit()
            .putString(KEY_PIN_HASH, hash(pin))
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    /**
     * 입력 PIN 검증 (Design.md 5.4).
     *
     * 성공하면 실패 횟수를 0으로 되돌리고, 실패하면 횟수를 올린다.
     * [MAX_FAIL_COUNT]회 연속 실패하면 [LOCKOUT_MS] 동안 입력을 막는다.
     */
    fun verify(context: Context, pin: String): Boolean {
        val saved = prefs(context).getString(KEY_PIN_HASH, null) ?: return false
        val matched = saved == hash(pin)
        if (matched) resetFailState(context) else recordFailure(context)
        return matched
    }

    /** 남은 입력 차단 시간(ms). 0이면 차단 중이 아니다. */
    fun remainingLockoutMs(context: Context): Long {
        val until = prefs(context).getLong(KEY_LOCK_UNTIL, 0L)
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    /** 현재까지 연속 실패한 횟수 */
    fun failCount(context: Context): Int = prefs(context).getInt(KEY_FAIL_COUNT, 0)

    private fun recordFailure(context: Context) {
        val p = prefs(context)
        val count = p.getInt(KEY_FAIL_COUNT, 0) + 1
        val editor = p.edit().putInt(KEY_FAIL_COUNT, count)
        if (count >= MAX_FAIL_COUNT) {
            // 차단 시작 — 다음 입력은 차단이 풀린 뒤부터 다시 셈한다
            editor.putLong(KEY_LOCK_UNTIL, System.currentTimeMillis() + LOCKOUT_MS)
                .putInt(KEY_FAIL_COUNT, 0)
        }
        editor.apply()
    }

    private fun resetFailState(context: Context) {
        prefs(context).edit()
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    /** 생체인증 사용 여부 — 앱 잠금이 켜져 있을 때만 의미가 있다. */
    fun isBiometricEnabled(context: Context): Boolean =
        isEnabled(context) && prefs(context).getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    /** 앱 잠금 해제(설정 + PIN + 실패 이력 제거). */
    fun disable(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_BIOMETRIC, false)
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
