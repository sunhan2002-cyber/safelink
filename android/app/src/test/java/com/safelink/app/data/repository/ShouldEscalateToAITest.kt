package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.MatchedKeyword
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * DetectionEngine.shouldEscalateToAI() 검증 - 신기훈 4주차 06번 문서 "AI API 진입 조건 확정본"
 * 5개 조건이 코드로 정확히 옮겨졌는지 확인.
 */
class ShouldEscalateToAITest {

    companion object {
        private lateinit var engine: DetectionEngine

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val keywordJson = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
            val institutionJson = javaClass.classLoader!!.getResourceAsStream("institutions.json")!!.bufferedReader().readText()
            val keywordData = gson.fromJson(keywordJson, KeywordData::class.java)
            val institutionData = gson.fromJson(institutionJson, InstitutionData::class.java)
            engine = DetectionEngine(keywordData, institutionData, gson)
        }

        private fun resultWithScore(score: Int, matched: List<MatchedKeyword> = emptyList()) = DetectionResult(
            riskLevel = RiskLevel.fromScore(score),
            score = score,
            category = "",
            originalText = "",
            matchedKeywords = matched,
            recommendedInstitutions = emptyList()
        )

        private fun matchOf(subcategoryId: String) = MatchedKeyword(
            keywordId = "TEST-$subcategoryId",
            subcategoryId = subcategoryId,
            subcategoryName = "",
            matchedText = "",
            startIndex = 0,
            endIndex = 0,
            weight = 0,
            description = ""
        )
    }

    @Test
    fun `수동 신고면 점수 무관 항상 호출`() {
        val result = resultWithScore(0)
        assertTrue(engine.shouldEscalateToAI(result, sessionTurnCount = 1, manualReportFlag = true))
    }

    @Test
    fun `회색지대 점수(20-40, 55-70)는 호출, 그 밖은 새 중분류 없으면 호출 안 함`() {
        assertTrue(engine.shouldEscalateToAI(resultWithScore(30), sessionTurnCount = 1))
        assertTrue(engine.shouldEscalateToAI(resultWithScore(60), sessionTurnCount = 1))
        assertFalse(engine.shouldEscalateToAI(resultWithScore(10), sessionTurnCount = 1, newSubcategoryRolloutActive = false))
        assertFalse(engine.shouldEscalateToAI(resultWithScore(90), sessionTurnCount = 1, newSubcategoryRolloutActive = false))
    }

    @Test
    fun `가스라이팅 어조의존 중분류(3-1,3-2,3-7,3-8,3-9) 2회 이상이면 점수 무관 호출`() {
        val matched = listOf(matchOf("3-1"), matchOf("3-7"))
        val result = resultWithScore(5, matched)
        assertTrue(engine.shouldEscalateToAI(result, sessionTurnCount = 1, newSubcategoryRolloutActive = false))
    }

    @Test
    fun `가스라이팅 어조의존 중분류 1회뿐이면 호출 안 함(다른 조건 없을 때)`() {
        val matched = listOf(matchOf("3-1"))
        val result = resultWithScore(5, matched)
        assertFalse(engine.shouldEscalateToAI(result, sessionTurnCount = 1, newSubcategoryRolloutActive = false))
    }

    @Test
    fun `FUTURE_FAKE-SUNK_COST는 세션 15턴 이상일 때만 호출`() {
        val matched = listOf(matchOf("2-10"))
        val result = resultWithScore(5, matched)
        assertFalse(engine.shouldEscalateToAI(result, sessionTurnCount = 5, newSubcategoryRolloutActive = false))
        assertTrue(engine.shouldEscalateToAI(result, sessionTurnCount = 15, newSubcategoryRolloutActive = false))
    }

    @Test
    fun `신규 중분류는 롤아웃 스위치 켜져있으면 점수 무관 호출, 꺼지면 호출 안 함`() {
        val matched = listOf(matchOf("2-6"))
        val result = resultWithScore(5, matched)
        assertTrue(engine.shouldEscalateToAI(result, sessionTurnCount = 1, newSubcategoryRolloutActive = true))
        assertFalse(engine.shouldEscalateToAI(result, sessionTurnCount = 1, newSubcategoryRolloutActive = false))
    }
}
