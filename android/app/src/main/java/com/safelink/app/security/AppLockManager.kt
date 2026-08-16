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

    /** 입력 PIN 이 저장된 PIN 과 일치하는지. */
    fun verify(context: Context, pin: String): Boolean {
        val saved = prefs(context).getString(KEY_PIN_HASH, null) ?: return false
        return saved == hash(pin)
    }

    /** 앱 잠금 해제(설정 + PIN 제거). */
    fun disable(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .apply()
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
