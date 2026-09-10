package com.safelink.app.settings

import android.content.Context

/**
 * 백그라운드 감지에서 AI 정밀 분석을 쓸지에 대한 사용자 동의.
 *
 * ── 왜 동의를 따로 받는가 ──────────────────────────────────────────────
 * 수동 분석은 사용자가 대화를 직접 붙여넣으며 "이걸 분석시킨다"는 걸 인지한다.
 * 백그라운드는 다르다. 화면에 떠 있는 내용을 사용자가 인지하지 못한 채 읽고,
 * 그 안에는 **상대방이 보낸 메시지**가 그대로 들어 있다.
 *
 * 온디바이스 판정은 기기 밖으로 나가는 게 없어서 문제가 없지만, AI 정밀 분석은
 * 대화 본문이 서버로 나간다. 사용자가 모르는 사이에 제3자 대화가 외부로 나가는 상황은
 * 만들지 않는다는 것이 이 앱의 전제라, **명시적으로 동의한 경우에만** 켜지게 한다.
 *
 * 그래서 기본값은 꺼짐이고, 켜는 순간 무엇이 전송되는지 그대로 보여준 뒤 동의를 받는다.
 * 동의 문구는 [CONSENT_BODY] 에 있다 — 화면에 보이는 약속이므로 구현이 바뀌면 여기도 같이 바꿔야 한다.
 */
object AiConsentStore {

    private const val PREFS = "safelink_settings"
    private const val KEY_ENABLED = "ai_background_consent"
    private const val KEY_AGREED_AT = "ai_background_consent_at"

    /**
     * 동의 화면에 그대로 띄우는 문구.
     *
     * 여기 적은 것은 전부 **구현으로 지킬 수 있는 것만** 적었다.
     * "어디에도 저장되지 않는다" 같은 문장은 AI 제공사 보관 정책까지 걸린 이야기라
     * 우리가 보장할 수 없어 쓰지 않았고, 대신 그 사실을 그대로 밝혔다.
     */
    const val CONSENT_TITLE = "AI 정밀 분석을 사용할까요?"

    const val CONSENT_BODY =
        "기기 안에서 판단하기 애매한 경우에 한해, 감지된 대화 내용이 분석 서버로 전송됩니다.\n\n" +
            "⚠️ 전송되는 내용에 상대방이 보낸 메시지가 포함됩니다. " +
            "전화번호와 링크는 가려서 보내지만, 대화 본문은 그대로 전송됩니다.\n\n" +
            "· 분석이 끝나면 대화 내용은 SafeLink 서버에 남지 않습니다\n" +
            "· 판정 결과(감지된 표현 유형·위험도)는 탐지 정확도 개선을 위한 검증 자료로만 활용됩니다\n" +
            "· 그 외의 목적으로는 일절 사용하지 않습니다\n" +
            "· AI 제공사에는 오·남용 확인 목적으로 일정 기간 보관될 수 있습니다\n\n" +
            "끄면 지금처럼 기기 안에서만 판단합니다."

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 백그라운드 감지에서 AI 정밀 분석을 써도 되는지. 기본값은 꺼짐. */
    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    /**
     * 동의한 시각(epoch millis). 동의한 적이 없으면 0.
     * 문구가 바뀌었을 때 재동의를 받아야 하는지 판단하는 근거로 남겨 둔다.
     */
    fun agreedAt(context: Context): Long =
        prefs(context).getLong(KEY_AGREED_AT, 0L)

    /** 동의 화면에서 "동의하고 사용"을 누른 경우에만 호출한다. */
    fun agree(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, true)
            .putLong(KEY_AGREED_AT, System.currentTimeMillis())
            .apply()
    }

    /** 사용자가 끈 경우. 동의 시각은 지워서 다시 켤 때 문구를 다시 보게 한다. */
    fun revoke(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_AGREED_AT)
            .apply()
    }
}
