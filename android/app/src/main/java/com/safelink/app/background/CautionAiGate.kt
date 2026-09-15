package com.safelink.app.background

/**
 * 백그라운드에서 "주의" 단계 대화를 AI 에 보낼지 정하는 문지기.
 *
 * ── 왜 주의 단계도 보내는가 ──────────────────────────────────────────
 * 규칙 엔진은 본 적 있는 표현에 강하고 처음 보는 표현에 약하다. 처음 보는 사기 문장 측정에서
 * 놓친 건 대부분 12~25점("주의")에 머물렀는데, 예전에는 경고(31점) 이상만 AI 에 보냈기 때문에
 * 이런 대화는 AI 동의를 켜도 AI 가 볼 기회가 없었다. 주의 단계는 "기기 안에서 판단하기 애매한 경우"라
 * 동의 문구([com.safelink.app.settings.AiConsentStore.CONSENT_BODY])의 범위 안이다.
 *
 * ── 왜 거르는가 ─────────────────────────────────────────────────────
 * 백그라운드 분석은 화면이 바뀔 때마다 돈다. 같은 대화방을 스크롤하거나 다시 열 때마다 AI 를 부르면
 * 같은 대화가 여러 번 밖으로 나가고 비용도 늘어난다. 그래서
 * - 같은 대화(앱 + 걸린 표현 묶음)는 [sameConversationMuteMs] 동안 다시 보내지 않고,
 * - 전체 호출 수를 [windowMs] 동안 [maxCallsPerWindow] 건으로 제한한다.
 *
 * 시간은 호출 측이 넘긴다(SystemClock) — JVM 유닛 테스트에서 시간을 직접 넣어 검증하기 위해서다.
 */
class CautionAiGate(
    private val sameConversationMuteMs: Long = 30 * 60 * 1000L,
    private val windowMs: Long = 60 * 60 * 1000L,
    private val maxCallsPerWindow: Int = 20,
    private val maxRemembered: Int = 50
) {
    private val lastSentAt = LinkedHashMap<String, Long>()
    private val recentCalls = ArrayDeque<Long>()

    /** 보내도 되면 true 를 돌려주고 보낸 것으로 기록한다. */
    @Synchronized
    fun tryAcquire(pkg: String, keywordIds: Set<String>, phrases: Set<String>, now: Long): Boolean {
        if (keywordIds.isEmpty()) return false
        val key = keyOf(pkg, keywordIds, phrases)

        lastSentAt[key]?.let { if (now - it < sameConversationMuteMs) return false }

        while (recentCalls.isNotEmpty() && now - recentCalls.first() >= windowMs) recentCalls.removeFirst()
        if (recentCalls.size >= maxCallsPerWindow) return false

        recentCalls.addLast(now)
        lastSentAt.remove(key)
        lastSentAt[key] = now
        while (lastSentAt.size > maxRemembered) lastSentAt.remove(lastSentAt.keys.first())
        return true
    }

    companion object {
        /**
         * 주의 → 경고로 올려 알리려면 AI 가 이만큼은 올려야 한다.
         * 실측(2026-09-15): 확인용 사기 문장 8건은 +20~+25, 일상·애매한 대화 22건은 1건(+5)을 빼고 전부 음수였다.
         * "정보가 부족하다"면서 +5 를 준 애매한 대화("영상 가지고 있으니까 나중에 보내줄게")가 28점에서
         * 경고로 넘어가는 걸 막기 위한 기준이다.
         */
        const val MIN_AI_RAISE = 10

        fun shouldAlert(ruleScore: Int, aiScore: Int): Boolean =
            aiScore >= WARNING_MIN_SCORE && aiScore - ruleScore >= MIN_AI_RAISE

        private const val WARNING_MIN_SCORE = 31
    }

    private fun keyOf(pkg: String, keywordIds: Set<String>, phrases: Set<String>): String =
        pkg + "|" + keywordIds.sorted().joinToString(",") + "|" + phrases.sorted().joinToString("")
}
