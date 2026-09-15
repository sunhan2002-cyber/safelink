package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * 띄어쓰기 무시 키워드 비교 (신기훈 탐지보강 변경정리 2-1, 김선한 구현작업 v1 5단계).
 * 키워드(match_type=keyword)는 공백을 빼고 비교하되, 짧은 키워드는 글자 그대로 일치만 본다.
 */
class KeywordSpacingTest {

    companion object {
        private lateinit var engine: DetectionEngine
        private lateinit var keywordData: KeywordData

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val kw = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
            val inst = javaClass.classLoader!!.getResourceAsStream("institutions.json")!!.bufferedReader().readText()
            keywordData = gson.fromJson(kw, KeywordData::class.java)
            engine = DetectionEngine(keywordData, gson.fromJson(inst, InstitutionData::class.java), gson)
        }
    }

    private fun matchedIds(text: String) = engine.analyze(text).matchedKeywords.map { it.keywordId }

    @Test
    fun `붙여 쓴 문장도 띄어 쓴 키워드를 잡는다`() {
        assertTrue("대신 송금", "FM-4-3-002" in matchedIds("급한데 대신송금해줄래?"))
        assertTrue("지금 통화가 안 돼", "FM-4-2-001" in matchedIds("지금통화가안돼"))
        assertTrue("keyword 타입: 안전계좌로 이체", "VP-1-5-002" in matchedIds("안전계좌로이체해"))
    }

    @Test
    fun `띄어 쓴 문장도 붙여 쓴 키워드를 잡는다`() {
        assertTrue("안전계좌", "VP-1-5-001" in matchedIds("안전 계좌로 옮기셔야 합니다"))
    }

    @Test
    fun `밑줄 위치와 matchedText 는 띄어쓰기가 그대로인 원문 기준이다`() {
        // 띄어쓰기 무시 비교는 keyword 타입에만 적용되므로, 정규식으로 전환되지 않은 항목(안전계좌로 이체)으로 확인한다
        val text = "지금 바로  안전 계좌로   이체 하세요"
        val m = engine.analyze(text).matchedKeywords.first { it.keywordId == "VP-1-5-002" }
        assertEquals("원문 그대로(공백 포함)", "안전 계좌로   이체", m.matchedText)
        assertEquals(text.indexOf("안전"), m.startIndex)
        assertEquals(text.indexOf("이체") + "이체".length, m.endIndex)
    }

    @Test
    fun `공백을 빼면 2글자 이하인 짧은 키워드는 띄어쓰기 무시 비교를 하지 않는다`() {
        val shortOnes = keywordData.keywords
            .filter { it.matchType == "keyword" && it.keyword!!.filterNot { c -> c == ' ' }.length < DetectionEngine.MIN_SPACE_INSENSITIVE_LENGTH }
            .map { it.keyword }
            .toSet()
        assertEquals(setOf("검사", "즉시", "압류", "구속", "옷 벗"), shortOnes)

        assertFalse("옷벗(붙여 씀)은 '옷 벗'으로 보지 않음", "TH-7-5-011" in matchedIds("옷벗어서 걸어둬"))
        assertFalse("검 사(띄어 씀)는 '검사'로 보지 않음", "VP-1-1-008" in matchedIds("검 사 받으러 가"))
        assertFalse("즉 시(띄어 씀)는 '즉시'로 보지 않음", "VP-1-4-002" in matchedIds("즉 시 가능"))
    }

    @Test
    fun `줄바꿈을 넘어서 두 메시지의 글자가 이어져 잡히지 않는다`() {
        // 한 턴 안의 줄바꿈은 메시지 경계라 공백으로 보지 않는다 (keyword 타입 기준)
        assertFalse("VP-1-5-002" in matchedIds("안전계좌로\n이체"))
    }
}
