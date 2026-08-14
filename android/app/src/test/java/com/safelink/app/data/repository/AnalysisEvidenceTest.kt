package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.AnalysisEvidence
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import com.safelink.app.data.remote.dto.AnalyzeResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * DetectionResult.evidences 분류 검증 (5주차 신설 - 결과 화면 "분석 근거" 데이터 정리,
 * AnalysisEvidence 참고). 근거를 키워드/문장 규칙/상황 규칙/AI 4종으로 정확히 구분해서
 * 담는지가 핵심 - 김우영이 화면 문구를 이 source 값 기준으로 나눠서 작업한다.
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
    fun `일반 키워드 매칭은 SOURCE_KEYWORD 근거로 잡힘`() {
        val result = engine.analyze("택배기사인데요, 배송 중 확인 차 연락드렸습니다.")
        assertTrue(result.evidences.any {
            it.source == AnalysisEvidence.SOURCE_KEYWORD && it.detail == "택배기사"
        })
    }

    @Test
    fun `regex-complex(문장 규칙) 매칭은 SOURCE_SENTENCE_RULE 근거로 잡힘`() {
        val result = engine.analyze("정 그러면 100만원만요. 그 이상은 안 돼요.")
        assertTrue(result.evidences.any {
            it.source == AnalysisEvidence.SOURCE_SENTENCE_RULE && it.detail == "100만원만"
        })
    }

    @Test
    fun `numeric_ratio_pattern 콤보도 SOURCE_SENTENCE_RULE 근거로 잡힘`() {
        val result = engine.analyze("3주만에 100이 137 됐죠?")
        assertTrue(result.evidences.any { it.source == AnalysisEvidence.SOURCE_SENTENCE_RULE })
    }

    @Test
    fun `repeat_pattern long_session_pattern 콤보는 SOURCE_SITUATIONAL_RULE 근거로 잡힘`() {
        val result = engine.analyze(listOf(
            "너 오늘 좀 예민한 거 같아, 별일도 아닌데 왜 그래?",
            "내가 언제 그렇게 말했어? 너 또 왜곡해서 기억하는 거야."
        ))
        assertTrue("COMBO-GL-REPEAT-PATTERN" in result.appliedComboIds)
        assertTrue(result.evidences.any { it.source == AnalysisEvidence.SOURCE_SITUATIONAL_RULE })
    }

    @Test
    fun `겹침 억제된 매칭은 근거에 중복으로 안 들어감`() {
        // GL-3-2-001(원문)과 GL-3-2-006(문구변형 regex)이 같은 구간을 중복 매칭 -
        // 점수 계산과 마찬가지로 근거도 억제된 쪽은 제외해야 함
        val result = engine.analyze("내가 언제 그렇게 말했어?")
        val keywordEvidenceCount = result.evidences.count { it.source == AnalysisEvidence.SOURCE_KEYWORD }
        assertEquals(1, keywordEvidenceCount)
    }

    @Test
    fun `mergeAiResponse는 SOURCE_AI 근거를 온디바이스 근거 뒤에 추가함`() {
        val onDevice = engine.analyze("계좌번호를 알려주셔야 확인이 가능합니다.")
        val beforeCount = onDevice.evidences.size
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
        assertEquals(beforeCount + 1, merged.evidences.size)
        val aiEvidence = merged.evidences.last()
        assertEquals(AnalysisEvidence.SOURCE_AI, aiEvidence.source)
        assertEquals("테스트 패턴", aiEvidence.label)
        assertEquals("테스트 요약", aiEvidence.detail)
    }
}
