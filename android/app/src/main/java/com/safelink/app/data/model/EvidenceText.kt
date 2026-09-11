package com.safelink.app.data.model

/**
 * 판정 근거 문구를 사용자에게 보여줄 형태로 다듬는다.
 *
 * keyword.json 의 description 과 조합 규칙의 condition 에는 개발하면서 적은 메모가 섞여 있다.
 * - "숫자로 한정한 소액 요구(SMALL_ASK) - 금액이 매번 달라 키워드로 전부 나열 불가능해 숫자 추출+임계값…"
 * - "전화번호 형식 - 단독 점수 없음, combo 트리거용"
 * - "전화번호(VP-1-3-003) 안내 + 확인/전화 유도 표현(VP-1-3-001, VP-1-3-002)이 1~2턴 이내 함께 등장"
 * - "가스라이팅 관련 중분류(3-1 DENY, 3-2 MEM_DOUBT, …) 매칭이 … - … 트리거 조건(shouldEscalateToAI)과 별개로 …"
 *
 * 이 문구가 결과 화면의 "분석 근거"에 그대로 떠서, 심사위원·사용자에게 내부 규칙 설명이 보였다.
 * 원본 파일은 규칙 엔진과 테스트가 그대로 쓰므로 고치지 않고, **화면에 표시하는 순간에만** 걸러낸다.
 */
object EvidenceText {

    /** 기법 코드가 하나라도 든 괄호: (SMALL_ASK), (3-1 DENY, 3-2 MEM_DOUBT, 3-9) */
    private val CODE_GROUP = Regex("""\s*\([^()]*\b[A-Z][A-Z_]{2,}\b[^()]*\)""")

    /** 규칙 id 가 들어간 괄호: (VP-1-3-001, VP-1-3-002), (VP-1-6-001~003) */
    private val RULE_ID_GROUP = Regex("""\s*\([^()]*\b[A-Z]{2}-\d+-\d+[^()]*\)""")

    /** 코드 식별자가 든 괄호: (shouldEscalateToAI) */
    private val IDENTIFIER_GROUP = Regex("""\s*\(\s*[a-z]+[A-Z][A-Za-z]*\s*\)""")

    /**
     * 사용자 원문에는 나올 리 없는 강한 개발 메모 표지. 이게 " - " 뒤에 있으면 항상 잘라낸다.
     * (camelCase·snake_case 식별자, any-of, combo 등)
     */
    private val STRONG_DEVELOPER_NOTE = Regex(
        """\b[a-z]+_[a-z_]+\b|\b[a-z]+[A-Z][A-Za-z]+\b|any-of|combo|트리거|임계값|메커니즘|단독 점수"""
    )

    /** 규칙 설명에서만 개발 메모로 보는 표지. 사용자 원문에도 나올 수 있어 [forUser] 의 cutDeveloperNote 가 true 일 때만 쓴다. */
    private val DEVELOPER_NOTE = Regex(
        """[A-Z][A-Z_]{2,}|\b[A-Z]{2}-\d+-\d+|매칭|판정|키워드|통합|추출|정규식|오탐"""
    )

    private val EXTRA_SPACES = Regex("""\s{2,}""")

    /**
     * @param cutDeveloperNote " - " 뒤가 개발 메모로 보이면 잘라낼지. 사용자가 입력한 원문(매칭된 문구)처럼
     *   원래 " - " 가 들어갈 수 있는 문자열에는 false 로 부른다 — 그래도 코드 식별자 같은 강한 표지는 자른다.
     */
    fun forUser(raw: String, cutDeveloperNote: Boolean = true): String {
        var text = raw
            .replace(RULE_ID_GROUP, "")
            .replace(CODE_GROUP, "")
            .replace(IDENTIFIER_GROUP, "")
        val separator = text.indexOf(" - ")
        if (separator >= 0) {
            val tail = text.substring(separator + 3)
            val isNote = STRONG_DEVELOPER_NOTE.containsMatchIn(tail) ||
                (cutDeveloperNote && DEVELOPER_NOTE.containsMatchIn(tail))
            if (isNote) text = text.substring(0, separator)
        }
        return text.replace(EXTRA_SPACES, " ").trim().trimEnd('-', ',', ' ')
    }
}
