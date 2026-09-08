package com.safelink.app.ui.screens.diagnosis

import com.safelink.app.data.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 자가진단 채점 로직 검증(Task 4.9).
 *
 * 기준: Design.md 5.1 — 고위험 2점 / 일반 1점, 백분율 70·40·10% 경계로 위험도 분류.
 * 문항이 추가·수정되어도 만점과 경계 동작이 스펙에서 벗어나지 않는지 함께 확인한다.
 */
class DiagnosisScoringTest {

    private fun scoreOf(vararg indices: Int): Int = DiagnosisScorer.scoreOf(indices.toList())

    @Test
    fun `문항 가중치는 고위험 2점 일반 1점이어야 함`() {
        checklistItems.forEach { item ->
            val expected = if (item.highRisk) 2 else 1
            assertEquals("가중치 불일치: ${item.text}", expected, item.weight)
        }
    }

    @Test
    fun `현재 문항 구성의 만점은 고위험 7개와 일반 5개 기준 19점`() {
        val highRisk = checklistItems.count { it.highRisk }
        val normal = checklistItems.count { !it.highRisk }
        assertEquals(7, highRisk)
        assertEquals(5, normal)
        assertEquals(19, checklistItems.sumOf { it.weight })
    }

    @Test
    fun `아무것도 체크하지 않으면 0점 안전`() {
        assertEquals(RiskLevel.SAFE, DiagnosisScorer.levelOf(0))
    }

    @Test
    fun `위험도 경계값은 70 40 10 기준을 따름`() {
        // 경계 바로 아래 / 경계값 자체를 함께 확인해 오분류를 막는다
        assertEquals(RiskLevel.SAFE, DiagnosisScorer.levelOf(9))
        assertEquals(RiskLevel.CAUTION, DiagnosisScorer.levelOf(10))
        assertEquals(RiskLevel.CAUTION, DiagnosisScorer.levelOf(39))
        assertEquals(RiskLevel.WARNING, DiagnosisScorer.levelOf(40))
        assertEquals(RiskLevel.WARNING, DiagnosisScorer.levelOf(69))
        assertEquals(RiskLevel.CRITICAL, DiagnosisScorer.levelOf(70))
        assertEquals(RiskLevel.CRITICAL, DiagnosisScorer.levelOf(100))
    }

    @Test
    fun `일반 항목 하나만 체크하면 주의 미만이라 안전`() {
        // 일반 1점 / 19점 = 5% → 10% 미만
        val score = scoreOf(4) // "의심스러운 링크 클릭을 유도받았다"(일반)
        assertEquals(5, score)
        assertEquals(RiskLevel.SAFE, DiagnosisScorer.levelOf(score))
    }

    @Test
    fun `고위험 항목 하나만 체크하면 주의`() {
        // 고위험 2점 / 19점 = 10% → CAUTION 경계
        val score = scoreOf(0) // "상대방이 급하게 돈을 보내라고 요구했다"(고위험)
        assertEquals(10, score)
        assertEquals(RiskLevel.CAUTION, DiagnosisScorer.levelOf(score))
    }

    @Test
    fun `고위험 4개를 체크하면 경고`() {
        // 8점 / 19점 = 42% → WARNING
        val score = scoreOf(0, 1, 2, 3)
        assertEquals(42, score)
        assertEquals(RiskLevel.WARNING, DiagnosisScorer.levelOf(score))
    }

    @Test
    fun `고위험 7개 전부 체크하면 긴급`() {
        // 14점 / 19점 = 73% → CRITICAL
        val highRiskIndices = checklistItems.indices.filter { checklistItems[it].highRisk }
        val score = scoreOf(*highRiskIndices.toIntArray())
        assertEquals(73, score)
        assertEquals(RiskLevel.CRITICAL, DiagnosisScorer.levelOf(score))
    }

    @Test
    fun `전체 체크 시 100점 긴급`() {
        val score = scoreOf(*checklistItems.indices.toList().toIntArray())
        assertEquals(100, score)
        assertEquals(RiskLevel.CRITICAL, DiagnosisScorer.levelOf(score))
    }

    @Test
    fun `resultOf는 점수 위험도 근거 체크수를 함께 산출하고 고위험 근거를 앞에 둔다`() {
        val checked = listOf(4, 0, 2) // 일반 1개 + 고위험 2개

        val result = DiagnosisScorer.resultOf(checked)

        assertEquals(3, result.checkedCount)
        assertEquals(scoreOf(0, 2, 4), result.score)
        assertEquals(DiagnosisScorer.levelOf(result.score), result.level)
        assertEquals(3, result.reasons.size)
        // 고위험 항목 2개가 앞에 오고, 일반 항목이 마지막
        assertEquals(checklistItems[4].reason, result.reasons.last())
        assertTrue(result.reasons.first() in listOf(checklistItems[0].reason, checklistItems[2].reason))
    }

    @Test
    fun `체크가 없으면 근거도 비어 있다`() {
        val result = DiagnosisScorer.resultOf(emptyList())

        assertEquals(0, result.score)
        assertEquals(0, result.checkedCount)
        assertTrue(result.reasons.isEmpty())
        assertEquals(RiskLevel.SAFE, result.level)
    }

    @Test
    fun `모든 문항은 화면 문구와 결과 문구를 각각 가진다`() {
        checklistItems.forEach { item ->
            assertTrue("문항 문구 누락", item.text.isNotBlank())
            assertTrue("결과 문구 누락: ${item.text}", item.reason.isNotBlank())
        }
    }
}
