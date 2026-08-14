package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import com.safelink.app.data.remote.dto.AnalyzeResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * DetectionResult.sentenceRuleEvidences/situationalRuleEvidences 검증 (6주차 구조 개선 -
 * 결과 화면 "분석 근거" 데이터를 문장 규칙/상황 규칙 전용 필드로 직접 분리, AnalysisEvidence
 * 참고). 김선한/신기훈 리뷰 반영: 태그로 섞인 단일 리스트가 아니라 필드 자체로 구분되므로
 * 화면 쪽에서 필터링이 필요 없다는 게 핵심 - 각 필드에 정확한 근거만 담기는지 확인한다.
 */
class AnalysisEvidenceTest {

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
    fun `일반 키워드 매칭만 있으면 문장상황규칙 근거 둘 다 비어있음`() {
        val result = engine.analyze("택배기사인데요, 배송 중 확인 차 연락드렸습니다.")
        assertTrue(result.matchedKeywords.isNotEmpty())
        assertTrue(result.sentenceRuleEvidences.isEmpty())
        assertTrue(result.situationalRuleEvidences.isEmpty())
    }

    @Test
    fun `regex-complex(문장 규칙) 매칭은 sentenceRuleEvidences에만 잡힘`() {
        val result = engine.analyze("정 그러면 100만원만요. 그 이상은 안 돼요.")
        assertTrue(result.sentenceRuleEvidences.any { it.detail == "100만원만" })
        assertTrue(result.situationalRuleEvidences.isEmpty())
    }

    @Test
    fun `numeric_ratio_pattern 콤보도 sentenceRuleEvidences에 잡힘`() {
        val result = engine.analyze("3주만에 100이 137 됐죠?")
        assertTrue("COMBO-RS-NUMERIC-RATIO" in result.appliedComboIds)
        assertTrue(result.sentenceRuleEvidences.isNotEmpty())
        assertTrue(result.situationalRuleEvidences.isEmpty())
    }

    @Test
    fun `repeat_pattern long_session_pattern 콤보는 situationalRuleEvidences에만 잡힘`() {
        val result = engine.analyze(listOf(
            "너 오늘 좀 예민한 거 같아, 별일도 아닌데 왜 그래?",
            "내가 언제 그렇게 말했어? 너 또 왜곡해서 기억하는 거야."
        ))
        assertTrue("COMBO-GL-REPEAT-PATTERN" in result.appliedComboIds)
        assertTrue(result.situationalRuleEvidences.isNotEmpty())
        assertTrue(result.sentenceRuleEvidences.isEmpty())
    }

    @Test
    fun `겹침 억제된 매칭은 문장규칙 근거에 중복으로 안 들어감`() {
        // GL-3-2-001(원문)과 GL-3-2-006(문구변형 regex)처럼 같은 구간이 중복 매칭되는 상황을
        // regex-complex 매칭에도 동일하게 검증 - 여긴 RS-2-9-003 하나뿐이라 중복 시나리오는
        // 아니지만, distinctBy(entry.id) 적용 자체를 확인하는 회귀 케이스로 남겨둠.
        val result = engine.analyze("100만원만요.")
        assertEquals(1, result.sentenceRuleEvidences.count { it.detail == "100만원만" })
    }

    @Test
    fun `mergeAiResponse는 sentenceRule situationalRule을 안 건드리고 aiSummary aiDetectedPattern만 채움`() {
        val onDevice = engine.analyze("정 그러면 100만원만요.")
        val beforeSentenceRule = onDevice.sentenceRuleEvidences
        val response = AnalyzeResponseDto(
            contextScoreAdjustment = 10.0,
            contextAnalysisSummary = "테스트 요약",
            contextDetectedPattern = "테스트 패턴",
            recommendedLevelOverride = null,
            matchedKeywordIds = emptyList(),
            recommendedInstitutions = emptyList(),
            analysisTimestamp = "2026-01-01T00:00:00+09:00"
        )
        val merged = engine.mergeAiResponse(onDevice, response)
        assertEquals(beforeSentenceRule, merged.sentenceRuleEvidences)
        assertTrue(merged.situationalRuleEvidences.isEmpty())
        assertEquals("테스트 요약", merged.aiSummary)
        assertEquals("테스트 패턴", merged.aiDetectedPattern)
    }
}
