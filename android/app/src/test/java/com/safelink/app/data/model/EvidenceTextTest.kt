package com.safelink.app.data.model

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [EvidenceText] 검증 — 실제 keyword.json 에 들어 있는 문구로 확인한다.
 */
class EvidenceTextTest {

    @Test
    fun `기법 코드는 지우고 사용자용 설명은 남긴다`() {
        assertEquals(
            "소액 선요구 - 큰 금액 요구 전 심리적 문턱 낮추기",
            EvidenceText.forUser("소액 선요구(SMALL_ASK) - 큰 금액 요구 전 심리적 문턱 낮추기")
        )
        assertEquals(
            "가짜 검증 유도 - 제공된 번호로 확인시켜 신뢰 확보",
            EvidenceText.forUser("가짜 검증 유도(VERIFY_TRAP) - 제공된 번호로 확인시켜 신뢰 확보")
        )
    }

    @Test
    fun `뒤에 붙은 개발 메모는 잘라낸다`() {
        assertEquals(
            "숫자로 한정한 소액 요구",
            EvidenceText.forUser(
                "숫자로 한정한 소액 요구(SMALL_ASK) - 금액이 매번 달라 키워드로 전부 나열 불가능해 숫자 추출+임계값(500만원 이하)으로 판정. 큰 금액이면 '소액'의 취지에 안 맞아 매칭 제외"
            )
        )
        assertEquals("전화번호 형식", EvidenceText.forUser("전화번호 형식 - 단독 점수 없음, combo 트리거용"))
        assertEquals(
            "상위기관 연결 유도",
            EvidenceText.forUser("상위기관 연결 유도(ESCALATE) - VERIFY_TRAP과 같은 메커니즘이라 1-3에 통합")
        )
    }

    @Test
    fun `코드만 붙은 문구는 코드만 지운다`() {
        assertEquals("개인정보 탈취 시도", EvidenceText.forUser("개인정보 탈취 시도(INFO_HARVEST)"))
    }

    @Test
    fun `원래 사용자용인 문구는 그대로 둔다`() {
        assertEquals("택배기사 사칭 접근", EvidenceText.forUser("택배기사 사칭 접근"))
    }

    @Test
    fun `조합 규칙 조건에서 규칙 id 괄호를 지운다`() {
        assertEquals(
            "전화번호 안내 + 확인/전화 유도 표현이 1~2턴 이내 함께 등장 (+15점)",
            EvidenceText.forUser(
                "전화번호(VP-1-3-003) 안내 + 확인/전화 유도 표현(VP-1-3-001, VP-1-3-002)이 1~2턴 이내 함께 등장 (+15점)",
                cutDeveloperNote = false
            )
        )
        assertEquals(
            "스미싱 명목 문구와 URL이 같은 메시지 내 동시 등장",
            EvidenceText.forUser("스미싱 명목 문구(VP-1-6-001~003)와 URL(VP-1-6-004)이 같은 메시지 내 동시 등장")
        )
    }

    @Test
    fun `코드가 나열된 괄호와 코드 식별자가 섞인 조건도 정리한다`() {
        assertEquals(
            "가스라이팅 관련 중분류 매칭이 같은 세션에서 합산 2회 이상 감지",
            EvidenceText.forUser(
                "가스라이팅 관련 중분류(3-1 DENY, 3-2 MEM_DOUBT, 3-7 BLAME_SHIFT, 3-8 DISCREDIT, 3-9) 매칭이 같은 세션에서 합산 2회 이상 감지 - 반복 자체가 심리적조종의 핵심 신호라 AI 보조분석 트리거 조건(shouldEscalateToAI)과 별개로 온디바이스 점수에도 반영. subcategory_ids는 any-of(하나라도 해당하면 카운트)",
                cutDeveloperNote = false
            )
        )
    }

    @Test
    fun `사용자 원문은 하이픈이 있어도 자르지 않는다`() {
        assertEquals("지금 당장 - 계좌 확인 부탁", EvidenceText.forUser("지금 당장 - 계좌 확인 부탁", cutDeveloperNote = false))
    }

    @Test
    fun `실제 keyword json 의 모든 설명에서 개발 메모가 사라진다`() {
        val json = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
        val root = JsonParser.parseString(json).asJsonObject
        val leftovers = Regex("""\([A-Z][A-Z_]{2,}\)|combo|트리거|임계값|메커니즘|단독 점수|[A-Z]{2}-\d+-\d+|[a-z]+_[a-z]+|should[A-Z]""")

        val descriptions = root.getAsJsonArray("keywords").map { it.asJsonObject.get("description").asString }
        val conditions = root.getAsJsonArray("combo_bonus_rules").map { it.asJsonObject.get("condition").asString }
        assertTrue("검사할 문구가 있어야 한다", descriptions.size > 200 && conditions.isNotEmpty())

        val dirty = descriptions.map { EvidenceText.forUser(it) }.filter { leftovers.containsMatchIn(it) } +
            conditions.map { EvidenceText.forUser(it, cutDeveloperNote = false) }.filter { leftovers.containsMatchIn(it) }
        assertTrue("정리 후에도 개발 메모가 남은 문구: ${dirty.joinToString(" || ")}", dirty.isEmpty())
    }
}
