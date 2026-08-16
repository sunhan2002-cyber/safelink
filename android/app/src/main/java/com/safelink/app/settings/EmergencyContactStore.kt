package com.safelink.app.settings

import android.content.Context

/**
 * 긴급 연락처(지인) + 긴급 문자 본문 저장.
 *
 * 긴급 화면의 "지인에게 긴급 문자 보내기"가 실제로 동작하려면 보낼 대상과 문구가 있어야 한다.
 * 설정에서 등록/수정하고, 여기 저장된 값을 긴급 문자(ACTION_SENDTO)에 채워 넣는다.
 * 지금은 SharedPreferences 를 쓰며, 실제 배포 시 EncryptedSharedPreferences 로 교체한다(Task 5.15).
 */
object EmergencyContactStore {

    private const val PREFS = "safelink_emergency"
    private const val KEY_NAME = "contact_name"
    private const val KEY_PHONE = "contact_phone"
    private const val KEY_MESSAGE = "sms_message"

    const val DEFAULT_MESSAGE = "도움이 필요해요. 연락 부탁해요."

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 등록된 연락처(이름, 전화번호). 없으면 null. */
    fun getContact(context: Context): EmergencyContact? {
        val p = prefs(context)
        val name = p.getString(KEY_NAME, null)
        val phone = p.getString(KEY_PHONE, null)
        return if (!name.isNullOrBlank() && !phone.isNullOrBlank()) EmergencyContact(name, phone) else null
    }

    fun setContact(context: Context, name: String, phone: String) {
        prefs(context).edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_PHONE, phone.trim())
            .apply()
    }

    fun clearContact(context: Context) {
        prefs(context).edit().remove(KEY_NAME).remove(KEY_PHONE).apply()
    }

    /** 긴급 문자 본문. 미설정 시 기본 문구. */
    fun getMessage(context: Context): String =
        prefs(context).getString(KEY_MESSAGE, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_MESSAGE

    fun setMessage(context: Context, message: String) {
        prefs(context).edit().putString(KEY_MESSAGE, message.trim()).apply()
    }
}

data class EmergencyContact(val name: String, val phone: String)
