package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * match_type: regex-complex 검증 (5주차 "문장 규칙" - 신기훈 5주차 결과물 참고).
 * RS-2-9-003("(\d+)\s?만원만", numeric_max=500)이 캡처된 숫자값에 따라 매칭 여부가
 * 갈리는지 확인한다 - 신규_기법_문장_수집_v1.json N-RS2-02(NUMERIC_PATTERN 갭) 해소 검증.
 */
class RegexComplexNumericPatternTest {

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
    }

    @Test
    fun `임계값 이하 금액은 소액한정 요구로 매칭`() {
        val result = engine.analyze("정 그러면 100만원만요. 그 이상은 안 돼요. 제가 옆에서 봐드릴게요.")
        assertTrue(result.matchedKeywords.any { it.keywordId == "RS-2-9-003" })
        assertTrue("category가 로맨스스캠이어야 함", result.category == "로맨스스캠")
    }

    @Test
    fun `경계값(500만원)은 매칭, 경계값 초과(501만원)는 매칭 안 됨`() {
        val boundary = engine.analyze("500만원만요.")
        assertTrue(boundary.matchedKeywords.any { it.keywordId == "RS-2-9-003" })

        val overBoundary = engine.analyze("501만원만요.")
        assertFalse(overBoundary.matchedKeywords.any { it.keywordId == "RS-2-9-003" })
    }

    @Test
    fun `임계값 크게 초과하는 금액은 매칭 안 됨(소액 요구 취지 아님)`() {
        val result = engine.analyze("정 그러면 1000만원만요. 그 이상은 안 돼요.")
        assertFalse(result.matchedKeywords.any { it.keywordId == "RS-2-9-003" })
        assertTrue("숫자패턴 미매칭이면 다른 매칭도 없어 score=0", result.score == 0)
    }
}
