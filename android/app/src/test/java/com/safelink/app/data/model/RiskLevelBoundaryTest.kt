package com.safelink.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RiskLevel.fromScore() 경계값 검사 (SAFE 0~15 / CAUTION 16~30 / WARNING 31~65 / CRITICAL 66~100).
 * 41개 케이스 + 샘플 5개 검증에는 정확히 경계에 걸치는 값이 몇 개 없었어서 별도로 확인.
 */
class RiskLevelBoundaryTest {

    @Test
    fun `경계값 0 15 16 30 31 65 66 100`() {
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(0))
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(15))
        assertEquals(RiskLevel.CAUTION, RiskLevel.fromScore(16))
        assertEquals(RiskLevel.CAUTION, RiskLevel.fromScore(30))
        assertEquals(RiskLevel.WARNING, RiskLevel.fromScore(31))
        assertEquals(RiskLevel.WARNING, RiskLevel.fromScore(65))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(66))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(100))
    }

    @Test
    fun `100 초과 값도 CRITICAL로 방어적으로 처리됨 (엔진에서 100 캡이 있어 정상 경로로는 발생 안 함)`() {
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(150))
    }

    @Test
    fun `음수 입력도 SAFE로 방어적으로 처리됨 (정상 경로로는 발생 안 함)`() {
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(-5))
    }
}
