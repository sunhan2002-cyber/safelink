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
}
