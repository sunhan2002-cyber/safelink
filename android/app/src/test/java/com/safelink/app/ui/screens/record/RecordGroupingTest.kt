package com.safelink.app.ui.screens.record

import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.repository.RecordItem
import com.safelink.app.data.repository.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordGroupingTest {

    private fun detection(id: String, category: String, level: RiskLevel, time: Long) = RecordItem(
        id = id, type = RecordType.DETECTION, timestamp = time, riskLevel = level, score = 0,
        title = "텍스트 입력 · $category", summary = "", memo = null, category = category
    )

    private fun diagnosis(id: String, level: RiskLevel, time: Long) = RecordItem(
        id = id, type = RecordType.DIAGNOSIS, timestamp = time, riskLevel = level, score = 0,
        title = "자가진단 결과", summary = "", memo = null
    )

    @Test
    fun `같은 위험 유형끼리 묶이고 묶음 안은 최신순이다`() {
        val groups = groupByRiskType(
            listOf(
                detection("a", "보이스피싱", RiskLevel.WARNING, 100),
                detection("b", "협박·갈취", RiskLevel.WARNING, 200),
                detection("c", "보이스피싱", RiskLevel.CAUTION, 300)
            )
        )
        val phishing = groups.first { it.title == "보이스피싱" }
        assertEquals(listOf("c", "a"), phishing.records.map { it.id })
        assertEquals(2, groups.size)
    }

    @Test
    fun `가장 위험했던 묶음이 먼저, 같으면 최근 묶음이 먼저 온다`() {
        val groups = groupByRiskType(
            listOf(
                detection("old-critical", "보이스피싱", RiskLevel.CRITICAL, 100),
                detection("new-warning", "투자사기", RiskLevel.WARNING, 900),
                detection("new-critical", "협박·갈취", RiskLevel.CRITICAL, 500)
            )
        )
        assertEquals(listOf("협박·갈취", "보이스피싱", "투자사기"), groups.map { it.title })
    }

    @Test
    fun `위험 신호 없는 검사와 자가진단은 수법 묶음과 섞이지 않는다`() {
        val groups = groupByRiskType(
            listOf(
                detection("safe", "", RiskLevel.SAFE, 100),
                diagnosis("diag", RiskLevel.WARNING, 200),
                detection("p", "보이스피싱", RiskLevel.WARNING, 50)
            )
        )
        assertEquals(setOf("안전 판정", "자가진단", "보이스피싱"), groups.map { it.title }.toSet())
        assertEquals(listOf("diag"), groups.first { it.title == "자가진단" }.records.map { it.id })
    }

    @Test
    fun `유형이 채워져 있어도 안전 판정이면 수법 묶음에 세지 않는다`() {
        val groups = groupByRiskType(
            listOf(
                detection("safe-but-tagged", "보이스피싱", RiskLevel.SAFE, 100),
                detection("real", "보이스피싱", RiskLevel.WARNING, 50)
            )
        )
        assertEquals(listOf("real"), groups.first { it.title == "보이스피싱" }.records.map { it.id })
        assertEquals(listOf("safe-but-tagged"), groups.first { it.title == "안전 판정" }.records.map { it.id })
    }

    @Test
    fun `위험도별 건수는 높은 위험도부터 0건을 빼고 센다`() {
        val group = groupByRiskType(
            listOf(
                detection("1", "보이스피싱", RiskLevel.CAUTION, 1),
                detection("2", "보이스피싱", RiskLevel.CRITICAL, 2),
                detection("3", "보이스피싱", RiskLevel.CRITICAL, 3)
            )
        ).single()
        assertEquals(listOf(RiskLevel.CRITICAL to 2, RiskLevel.CAUTION to 1), group.countsByRisk)
        assertEquals(RiskLevel.CRITICAL, group.highestRisk)
    }
}
