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
 * 기준 + 5주차 정리(09번 문서: 가스라이팅 반복/장기세션 트리거는 온디바이스 점수 콤보로
 * 옮겨서 여기서 제거 - 회색지대·수동신고·신규subcategory 3개 조건만 남음).
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
        assertTrue(engine.shouldEscalateToAI(result, manualReportFlag = true))
    }

    @Test
    fun `회색지대 점수(20-40, 55-70)는 호출, 그 밖은 새 중분류 없으면 호출 안 함`() {
        assertTrue(engine.shouldEscalateToAI(resultWithScore(30)))
        assertTrue(engine.shouldEscalateToAI(resultWithScore(60)))
        assertFalse(engine.shouldEscalateToAI(resultWithScore(10), newSubcategoryRolloutActive = false))
        assertFalse(engine.shouldEscalateToAI(resultWithScore(90), newSubcategoryRolloutActive = false))
    }

    // 5주차 정리로 제거된 케이스: 가스라이팅 반복 2회+/장기세션 15턴+ 트리거는
    // COMBO-GL-REPEAT-PATTERN/COMBO-RS-LONG-SESSION-PATTERN(온디바이스 점수 콤보,
    // DetectionEngineTest의 TC-COMBO-07/08)로 이미 검증됨 - 09번 문서 참고.

    @Test
    fun `신규 중분류는 롤아웃 스위치 켜져있으면 점수 무관 호출, 꺼지면 호출 안 함`() {
        val matched = listOf(matchOf("2-6"))
        val result = resultWithScore(5, matched)
        assertTrue(engine.shouldEscalateToAI(result, newSubcategoryRolloutActive = true))
        assertFalse(engine.shouldEscalateToAI(result, newSubcategoryRolloutActive = false))
    }
}
