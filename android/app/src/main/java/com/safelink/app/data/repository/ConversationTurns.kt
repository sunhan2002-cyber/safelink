package com.safelink.app.data.repository

/**
 * 대화 원문을 "턴"(메시지 한 줄)으로 나눈다.
 *
 * ── 왜 필요한가 ────────────────────────────────────────────────────────
 * 규칙 엔진은 원래 여러 턴을 받도록 만들어져 있다(반복 패턴, 장기 세션 콤보, 턴 단위 매칭).
 * 그런데 지금까지 호출부는 대화 전체를 **한 덩어리 문자열 하나**로 넘겨서, 세션 신호를 쓰는 규칙
 * (COMBO-GL-REPEAT-PATTERN, COMBO-RS-LONG-SESSION-PATTERN 등)이 사실상 발동하지 못했다.
 * 붙여넣은 대화도, 백그라운드에서 읽은 화면도 줄 단위로 메시지가 나뉘므로 줄바꿈으로 턴을 나눈다.
 *
 * ── 나눌 때의 원칙 ─────────────────────────────────────────────────────
 * - 빈 줄은 버린다. 앞뒤 공백도 정리한다.
 * - 한 줄만 있으면 나누지 않고 원문 그대로 한 턴으로 둔다(기존 동작과 같음).
 * - 너무 많은 줄·너무 긴 줄은 잘라낸다. 화면 하나에서 수백 줄이 들어와도 분석 비용이 폭주하지 않게 한다.
 *   자를 때는 **최근 줄**을 남긴다 — 대화는 아래쪽이 최신이라 위험 신호도 대개 끝에 있다.
 */
object ConversationTurns {

    /** 한 번에 볼 최대 턴 수 */
    const val MAX_TURNS = 40

    /** 턴 하나의 최대 길이 */
    const val MAX_TURN_CHARS = 1000

    /** AI 보조분석에 함께 보낼 최근 턴 수 (요청이 지나치게 길어지지 않도록) */
    const val AI_RECENT_TURNS = 10

    fun split(text: String): List<String> {
        val lines = text.split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (lines.size <= 1) return listOf(text.trim().ifBlank { text })
        return lines.takeLast(MAX_TURNS).map { it.take(MAX_TURN_CHARS) }
    }

    /** AI 에 보낼 최근 턴만 추린다. */
    fun recentForAi(turns: List<String>): List<String> = turns.takeLast(AI_RECENT_TURNS)

    /**
     * AI 에 "분석할 대화"로 보낼 본문. 최근 턴 전체를 줄바꿈으로 이어 붙인다.
     *
     * 예전에는 마지막 한 줄만 분석 대상으로, 나머지 줄은 "이전 대화"로 나눠 보냈다. 그러면 AI 가
     * "이전 대화에서는 ~ 이번 대화는 일상적"처럼 마지막 줄 위주로 판단해, 사기 요구가 중간 줄에 있고
     * 마지막 줄이 평범한 대화("…송금해줘 / 이따 연락할게")를 낮게 볼 수 있었다.
     * 화면에 함께 보인 메시지들은 모두 지금 판단할 대화다.
     */
    fun aiConversation(maskedTurns: List<String>): String =
        maskedTurns.filter { it.isNotBlank() }.joinToString("\n")
}
