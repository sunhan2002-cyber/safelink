package com.safelink.app.settings

import android.content.Context

/**
 * 알림 문구 설정 (Task 5.15, Design.md 7장).
 *
 * ── 왜 문구를 바꿀 수 있게 하는가 ────────────────────────────────────────
 * 가해자와 같은 공간에 있거나 휴대폰을 함께 보는 상황에서는 알림 문구 자체가 위험이 될 수 있다.
 * Design.md 7장은 "알림 문구에 위험·폭력·상담 등 민감 단어를 넣지 않는다"와
 * "설정 화면에서 사용자가 직접 수정할 수 있다"를 규정한다.
 *
 * 사용자가 따로 설정하지 않으면 기본값([DEFAULT_TITLE]/[DEFAULT_BODY])을 쓰고,
 * 직접 입력하면 그 문구가 모든 위험도의 알림에 그대로 쓰인다.
 */
object NotificationTextStore {

    private const val PREFS = "safelink_settings"
    private const val KEY_USE_CUSTOM = "neutral_notif_enabled"
    private const val KEY_TITLE = "neutral_notif_title"
    private const val KEY_BODY = "neutral_notif_body"

    /** Design.md 7장 기본값 */
    const val DEFAULT_TITLE = "알림"
    const val DEFAULT_BODY = "새로운 알림이 있습니다"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 사용자가 지정한 중립 문구를 쓰는 중인지 */
    fun isCustomEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_CUSTOM, false)

    fun title(context: Context): String =
        prefs(context).getString(KEY_TITLE, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE

    fun body(context: Context): String =
        prefs(context).getString(KEY_BODY, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_BODY

    /**
     * 중립 문구 사용 여부와 내용을 저장한다.
     * 빈 값을 넘기면 기본값으로 되돌린다.
     */
    fun save(context: Context, enabled: Boolean, title: String, body: String) {
        prefs(context).edit()
            .putBoolean(KEY_USE_CUSTOM, enabled)
            .putString(KEY_TITLE, title.trim().takeIf { it.isNotBlank() } ?: DEFAULT_TITLE)
            .putString(KEY_BODY, body.trim().takeIf { it.isNotBlank() } ?: DEFAULT_BODY)
            .apply()
    }
}
