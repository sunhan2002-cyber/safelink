package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * 5주차 "문장 규칙"(신기훈 5주차 결과물 참고) 숫자 조건부 규칙 2종 검증.
 * 1) match_type: regex-complex - RS-2-9-003("(\d+)\s?만원만", numeric_max=500)이 캡처된
 *    숫자값에 따라 매칭 여부가 갈리는지 (NUMERIC_PATTERN 갭, N-RS2-02 해소 검증)
 * 2) combo_bonus_rules type: numeric_ratio_pattern - COMBO-RS-NUMERIC-RATIO가 원문의 두
 *    숫자 증가율을 계산해서 판정하는지 (NUMERIC_REASONING 갭 스트레치, N-RS2-03 관련)
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

    // COMBO-RS-NUMERIC-RATIO(numeric_ratio_pattern) 검증 - NUMERIC_REASONING 갭 스트레치 대응.
    // 키워드 매칭이 아니라 원문의 두 숫자 관계(증가율)만 보는 규칙이라 matchedKeywords는
    // 항상 비어있고 appliedComboIds/score에만 반영된다(신규_기법_문장_수집_v1.json N-RS2-03
    // 참고 - 그 코퍼스는 keyword id 매칭 기준이라 여전히 known_gap으로 남아있음).

    @Test
    fun `짧은 기간 고수익률 언급(15% 이상)이면 콤보 발동`() {
        val result = engine.analyze("3주만에 100이 137 됐죠?")
        assertTrue("COMBO-RS-NUMERIC-RATIO" in result.appliedComboIds)
        assertTrue("키워드 매칭 없이 콤보만으로 점수가 생김", result.score > 0)
        assertTrue("키워드 매칭 자체는 없음(순수 숫자 관계 판정)", result.matchedKeywords.isEmpty())
    }

    @Test
    fun `정상 범위 증가율(15% 미만)이면 콤보 미발동`() {
        val result = engine.analyze("3주만에 100이 105 됐죠?")
        assertFalse("COMBO-RS-NUMERIC-RATIO" in result.appliedComboIds)
    }

    @Test
    fun `숫자가 감소하는 경우는 콤보 미발동`() {
        val result = engine.analyze("100이 90 됐어요.")
        assertFalse("COMBO-RS-NUMERIC-RATIO" in result.appliedComboIds)
    }
}
