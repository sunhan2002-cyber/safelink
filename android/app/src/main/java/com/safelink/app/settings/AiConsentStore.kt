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
    private const val KEY_MANUAL_ENABLED = "ai_manual_consent"
    private const val KEY_MANUAL_AGREED_AT = "ai_manual_consent_at"

    /**
     * 동의 화면에 그대로 띄우는 문구.
     *
     * 여기 적은 것은 전부 **구현이나 원문 근거로 뒷받침되는 것만** 적었다.
     *
     * - "학습에 사용하지 않음": Anthropic API 데이터 보관 문서 원문
     *   "Retained data is never used for model training without your express permission."
     *   (platform.claude.com/docs/en/manage-claude/api-and-data-retention)
     * - "일정 기간 보관될 수 있음": 같은 회사 문서끼리 표현이 다르다(API 문서는 기본 미보관,
     *   개인정보센터는 30일 내 삭제, 정책 위반 플래그 시 최대 2년). 이 앱은 사기·협박 문구 자체를
     *   보내므로 안전 분류기에 걸릴 가능성을 배제할 수 없어, 어느 쪽이 맞든 참인 표현을 쓴다.
     * - "어디에도 저장되지 않는다"는 무보관(ZDR) 계약 없이는 보장할 수 없어 쓰지 않았다.
     *
     * 모델 제공사를 바꾸면 이 문구도 반드시 다시 확인해야 한다.
     */
    const val CONSENT_TITLE = "AI 보조분석을 사용할까요?"

    const val CONSENT_BODY =
        "기기 안에서 판단하기 애매한 경우에 한해, 감지된 대화 내용이 AI 제공사(Anthropic)로 전송됩니다.\n\n" +
            "⚠️ 전송되는 내용에 상대방이 보낸 메시지가 포함됩니다. " +
            "전화번호·계좌번호·주민등록번호·카드번호·이메일은 가리고, 링크는 도메인만 남긴 채 보냅니다. " +
            "다만 금액과 이름은 판단에 필요해 그대로 전송됩니다.\n\n" +
            "· 전송된 대화 내용을 SafeLink가 따로 수집하거나 보관하지 않습니다\n" +
            "· 분석 외의 목적으로는 일절 사용하지 않습니다\n" +
            "· AI 제공사(Anthropic)는 이 내용을 모델 학습에 사용하지 않으며, 오·남용 확인 목적으로 일정 기간 보관될 수 있습니다\n\n" +
            "끄면 지금처럼 기기 안에서만 판단합니다."

    /**
     * 직접 분석(대화 붙여넣기·스크린샷)에서 판단이 애매할 때 AI 를 자동으로 부를지에 대한 동의.
     *
     * 직접 분석은 사용자가 "이걸 검사한다"는 건 알지만, 그 내용이 **외부로 나간다**는 것까지 아는 건
     * 아니다. 예전에는 회색지대 점수(20~40, 55~70)에 걸리면 아무 안내 없이 자동으로 전송됐다.
     * 지금은 동의하기 전까지 자동 호출을 하지 않고, 결과 화면의 "AI 보조분석 요청"으로 한 건씩만 받는다
     * (그 버튼에는 무엇이 전송되는지 문구가 붙어 있다).
     */
    const val MANUAL_CONSENT_TITLE = "직접 분석에도 AI 보조분석을 쓸까요?"

    const val MANUAL_CONSENT_BODY =
        "켜면 기기 안에서 판단하기 애매한 경우에 한해, 분석한 대화 내용이 AI 제공사(Anthropic)로 자동 전송됩니다.\n\n" +
            "전화번호·계좌번호·주민등록번호·카드번호·이메일은 가리고, 링크는 도메인만 남긴 채 보냅니다. " +
            "다만 금액과 이름은 판단에 필요해 그대로 전송됩니다.\n\n" +
            "· 전송된 대화 내용을 SafeLink가 따로 수집하거나 보관하지 않습니다\n" +
            "· AI 제공사는 이 내용을 모델 학습에 사용하지 않으며, 오·남용 확인 목적으로 일정 기간 보관될 수 있습니다\n\n" +
            "끄면 기기 안에서만 판단하고, 필요할 때 결과 화면에서 직접 요청할 수 있습니다."

    /** 직접 분석에서 AI 를 자동으로 불러도 되는지. 기본값은 꺼짐. */
    fun isManualEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MANUAL_ENABLED, false)

    fun agreeManual(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_MANUAL_ENABLED, true)
            .putLong(KEY_MANUAL_AGREED_AT, System.currentTimeMillis())
            .apply()
    }

    fun revokeManual(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_MANUAL_ENABLED, false)
            .remove(KEY_MANUAL_AGREED_AT)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 백그라운드 감지에서 AI 보조분석을 써도 되는지. 기본값은 꺼짐. */
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
