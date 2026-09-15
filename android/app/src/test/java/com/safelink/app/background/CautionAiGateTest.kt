package com.safelink.app.background

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CautionAiGateTest {

    private val kakao = "com.kakao.talk"
    private val ids = setOf("FM-4-1-101")
    private val phrases = setOf("폰 액정")

    @Test
    fun `같은 대화는 정해진 시간 동안 다시 보내지 않는다`() {
        val gate = CautionAiGate(sameConversationMuteMs = 1_000)
        assertTrue(gate.tryAcquire(kakao, ids, phrases, now = 0))
        assertFalse("스크롤로 같은 화면을 다시 읽음", gate.tryAcquire(kakao, ids, phrases, now = 500))
        assertTrue("시간이 지나면 다시 보냄", gate.tryAcquire(kakao, ids, phrases, now = 1_000))
    }

    @Test
    fun `걸린 표현이 달라지면 새 대화로 보고 보낸다`() {
        val gate = CautionAiGate()
        assertTrue(gate.tryAcquire(kakao, ids, phrases, now = 0))
        assertTrue("새 메시지가 와서 표현이 늘어남", gate.tryAcquire(kakao, ids + "FM-4-3-104", phrases + "대신 송금", now = 10))
        assertTrue("다른 앱", gate.tryAcquire("com.instagram.android", ids, phrases, now = 20))
    }

    @Test
    fun `시간당 호출 수를 넘으면 보내지 않는다`() {
        val gate = CautionAiGate(windowMs = 1_000, maxCallsPerWindow = 2)
        assertTrue(gate.tryAcquire(kakao, setOf("A"), phrases, now = 0))
        assertTrue(gate.tryAcquire(kakao, setOf("B"), phrases, now = 100))
        assertFalse("한도 초과", gate.tryAcquire(kakao, setOf("C"), phrases, now = 200))
        assertTrue("창이 지나면 다시 가능", gate.tryAcquire(kakao, setOf("C"), phrases, now = 1_000))
    }

    @Test
    fun `10점 이상 경고 미만만 AI 로 보낸다`() {
        assertFalse("표현이 거의 없음", CautionAiGate.isCandidate(9))
        assertTrue(CautionAiGate.isCandidate(10))
        assertTrue(CautionAiGate.isCandidate(30))
        assertFalse("경고 이상은 알림 경로에서 따로 처리", CautionAiGate.isCandidate(31))
    }

    @Test
    fun `AI 가 뚜렷하게 올렸을 때만 알린다`() {
        assertTrue("사기 문장 실측: 20점 +20", CautionAiGate.shouldAlert(ruleScore = 20, aiScore = 40))
        assertFalse("애매한 대화 실측: 28점 +5 → 33점이지만 근거 약함", CautionAiGate.shouldAlert(ruleScore = 28, aiScore = 33))
        assertFalse("많이 올렸어도 경고 점수에 못 미침", CautionAiGate.shouldAlert(ruleScore = 16, aiScore = 30))
        assertFalse("일상 대화 실측 최대치: 20점 +15", CautionAiGate.shouldAlert(ruleScore = 20, aiScore = 35))
        assertTrue("경계: 15점 +16", CautionAiGate.shouldAlert(ruleScore = 15, aiScore = 31))
    }

    @Test
    fun `걸린 표현이 없으면 보내지 않는다`() {
        assertFalse(CautionAiGate().tryAcquire(kakao, emptySet(), emptySet(), now = 0))
    }
}
