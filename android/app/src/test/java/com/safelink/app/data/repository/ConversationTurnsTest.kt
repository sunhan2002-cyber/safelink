package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * [ConversationTurns] 와 "여러 줄 대화를 턴으로 나눠 분석" 동작 검증.
 *
 * 예전에는 붙여넣은 대화 전체를 한 덩어리로 넘겨서, 세션 단위 규칙(반복 패턴 등)이 발동하지 못했다.
 */
class ConversationTurnsTest {

    @Test
    fun `줄바꿈으로 턴을 나눈다`() {
        val turns = ConversationTurns.split("첫 줄\n둘째 줄\n\n  셋째 줄  ")
        assertEquals(listOf("첫 줄", "둘째 줄", "셋째 줄"), turns)
    }

    @Test
    fun `한 줄이면 원문 그대로 한 턴`() {
        assertEquals(listOf("한 줄짜리 대화"), ConversationTurns.split("한 줄짜리 대화"))
    }

    @Test
    fun `줄이 아주 많으면 최근 것만 남긴다`() {
        val turns = ConversationTurns.split((1..200).joinToString("\n") { "메시지 $it" })
        assertEquals(ConversationTurns.MAX_TURNS, turns.size)
        assertEquals("메시지 200", turns.last())
    }

    @Test
    fun `AI 로는 최근 10턴만 보낸다`() {
        val turns = (1..30).map { "메시지 $it" }
        val recent = ConversationTurns.recentForAi(turns)
        assertEquals(ConversationTurns.AI_RECENT_TURNS, recent.size)
        assertEquals("메시지 30", recent.last())
    }

    @Test
    fun `여러 줄 대화는 턴 단위로 분석돼 반복 패턴이 잡힌다`() {
        // 같은 중분류가 여러 턴에서 반복되면 반복 콤보(COMBO-*-REPEAT-*)가 발동해야 한다.
        val conversation = """
            내가 언제 그런 말을 했어?
            그런 적 없어. 네가 잘못 기억하는 거야.
            너 때문에 이렇게 된 거잖아.
            또 과민반응이네.
        """.trimIndent()

        val asOneChunk = engine.analyze(listOf(conversation))
        val asTurns = engine.analyze(ConversationTurns.split(conversation))

        assertTrue("턴으로 나누면 반복 콤보가 발동해야 한다", asTurns.appliedComboIds.any { it.contains("REPEAT") })
        assertTrue("턴으로 나눈 쪽 점수가 낮으면 안 된다", asTurns.score >= asOneChunk.score)
    }

    @Test
    fun `턴으로 나눠도 원문과 매칭 위치가 어긋나지 않는다`() {
        val conversation = "안녕하세요\n검찰청 수사관입니다\n지금 당장 계좌로 송금해 주세요"
        val result = engine.analyze(ConversationTurns.split(conversation))

        assertEquals("원문은 줄바꿈까지 그대로 유지된다", conversation, result.originalText)
        result.matchedKeywords.forEach { kw ->
            assertTrue("매칭 구간이 원문 범위 안에 있어야 한다", kw.endIndex <= result.originalText.length)
            assertEquals(
                "매칭 구간의 실제 글자가 matchedText 와 같아야 한다",
                kw.matchedText,
                result.originalText.substring(kw.startIndex, kw.endIndex)
            )
        }
        assertFalse("키워드가 하나도 안 잡히면 검증 의미가 없다", result.matchedKeywords.isEmpty())
    }

    companion object {
        private lateinit var engine: DetectionEngine

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val assets = File("src/main/assets")
            val keywordData = gson.fromJson(
                File(assets, "keyword.json").readText(),
                KeywordData::class.java
            )
            val institutionData = gson.fromJson(
                File(assets, "institutions.json").readText(),
                InstitutionData::class.java
            )
            engine = DetectionEngine(keywordData, institutionData, gson)
        }
    }
}
