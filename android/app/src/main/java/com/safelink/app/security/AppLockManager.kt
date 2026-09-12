package com.safelink.app.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 앱 잠금(PIN) 실제 동작 관리.
 *
 * - 설정에서 앱 잠금을 켜면 4자리 PIN 을 저장하고, 앱 실행 시 잠금 화면을 거치게 한다.
 * - PIN 은 평문 대신 **기기마다 다른 무작위 솔트 + PBKDF2** 로 저장한다.
 *   4자리 숫자는 경우의 수가 1만 개뿐이라, 솔트 없는 단순 SHA-256 은 저장값만 보면
 *   미리 계산한 표로 즉시 되돌릴 수 있었다. 반복 연산을 넣어 한 번의 대조에도 비용이 들게 한다.
 *   예전 방식(솔트 없는 SHA-256)으로 저장된 PIN 은 다음 로그인 성공 시 자동으로 새 방식으로 바꿔 둔다.
 * - 저장소는 지금은 SharedPreferences 를 쓴다. 실제 배포 시 EncryptedSharedPreferences 로
 *   교체하면 되며(같은 키/인터페이스 유지), 이 클래스만 바꾸면 화면 코드는 그대로 동작한다(Task 5.15).
 */
object AppLockManager {

    private const val PREFS = "safelink_security"
    private const val KEY_ENABLED = "app_lock_enabled"
    private const val KEY_PIN_HASH = "app_lock_pin_hash"
    private const val KEY_PIN_SALT = "app_lock_pin_salt"
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
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs(context).edit()
            .putString(KEY_PIN_SALT, salt.toBase64())
            .putString(KEY_PIN_HASH, pbkdf2(pin, salt))
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
        val prefs = prefs(context)
        val saved = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, null)?.fromBase64()

        val matched = if (salt != null) {
            constantTimeEquals(saved, pbkdf2(pin, salt))
        } else {
            // 예전 방식(솔트 없는 SHA-256)으로 저장된 PIN — 맞으면 새 방식으로 다시 저장한다.
            constantTimeEquals(saved, legacyHash(pin)).also { if (it) setPin(context, pin) }
        }

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
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_BIOMETRIC, false)
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    /** 저장용 해시 — 솔트를 섞어 [ITERATIONS] 번 반복한다. */
    private fun pbkdf2(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return key.encoded.joinToString("") { "%02x".format(it) }
    }

    /** 예전 저장 방식(솔트 없는 SHA-256) — 기존 사용자의 PIN 을 한 번 더 받기 위해서만 남겨 둔다. */
    private fun legacyHash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** 문자열 비교 시간으로 정답을 추측할 수 없도록 길이와 무관하게 같은 시간에 비교한다. */
    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray? = runCatching { Base64.decode(this, Base64.NO_WRAP) }.getOrNull()

    /** 4자리 PIN 이라 반복 횟수로 시간을 벌어야 한다. 기기에서 한 번 대조에 수십 ms 수준. */
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
}
