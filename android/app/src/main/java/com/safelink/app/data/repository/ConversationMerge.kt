package com.safelink.app.data.repository

/**
 * 백그라운드 감지 두 건이 "같은 대화가 이어진 것"인지 판단한다 — 기록을 새로 만들지 이어서 갱신할지 정하는 기준.
 *
 * ── 왜 필요한가 ────────────────────────────────────────────────────────
 * 메시지가 하나 올 때마다 화면이 바뀌어 다시 감지되고, 그때마다 기록이 새로 쌓였다. 실기기에서 인스타 로맨스스캠
 * 4줄이 3초 사이 기록 3건, 디스코드 가족사칭 6줄이 기록 2건으로 남아 기록 탭이 같은 대화로 채워졌다.
 *
 * ── 판단 기준 ──────────────────────────────────────────────────────────
 * 두 원문에서 **충분히 긴 줄**(6자 이상 — "재겸", "노트-자원" 같은 이름·방 이름 줄은 다른 대화에도 겹친다)을 뽑아,
 * 짧은 쪽 줄의 절반 이상이 긴 쪽에도 있으면 같은 대화로 본다. 새 메시지가 붙은 경우(예전 줄 ⊂ 새 줄)와
 * 스크롤로 위쪽 줄이 빠진 경우를 모두 같은 대화로 묶는다. 시간 조건은 호출하는 쪽([RecordRepository])이 건다.
 */
object ConversationMerge {

    private const val MIN_LINE_LENGTH = 6
    private const val MIN_OVERLAP_RATIO = 0.5

    fun isSameConversation(previous: String, current: String): Boolean {
        val a = meaningfulLines(previous)
        val b = meaningfulLines(current)
        if (a.isEmpty() || b.isEmpty()) return false
        val common = a.intersect(b).size
        return common >= 1 && common >= minOf(a.size, b.size) * MIN_OVERLAP_RATIO
    }

    private fun meaningfulLines(text: String): Set<String> =
        text.split('\n').map { it.trim() }.filter { it.length >= MIN_LINE_LENGTH }.toSet()
}
