package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** 공식 택배사 안내는 스미싱 조합으로 경고가 뜨지 않고, 흉내 낸 주소는 그대로 잡혀야 한다. */
class OfficialLinkScoringTest {

    companion object {
        private lateinit var engine: DetectionEngine

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val kw = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
            val inst = javaClass.classLoader!!.getResourceAsStream("institutions.json")!!.bufferedReader().readText()
            engine = DetectionEngine(gson.fromJson(kw, KeywordData::class.java), gson.fromJson(inst, InstitutionData::class.java), gson)
        }
    }

    @Test
    fun `진짜 택배사 주소는 스미싱 조합이 발동하지 않고 위험 표현으로도 표시하지 않는다`() {
        val r = engine.analyze("택배 조회는 아래 링크에서 가능합니다 https://www.cjlogistics.com")
        assertFalse(r.appliedComboIds.contains("COMBO-VP-SMISHING"))
        assertFalse(r.matchedKeywords.any { it.matchedText.contains("cjlogistics") })
        assertEquals("택배 조회 문구만 남는다", 25, r.score)
    }

    @Test
    fun `택배사를 흉내 낸 주소는 기존처럼 스미싱 조합으로 잡는다`() {
        val r = engine.analyze("택배 조회는 아래 링크에서 가능합니다 https://cjlogistics.com.kr-track.top/a")
        assertTrue(r.appliedComboIds.contains("COMBO-VP-SMISHING"))
        assertEquals(45, r.score)
    }

    @Test
    fun `공식 주소와 수상한 주소가 같이 있으면 수상한 주소 때문에 조합이 발동한다`() {
        val r = engine.analyze("택배 조회 https://www.cjlogistics.com 또는 http://bit.ly/xyz123")
        assertTrue(r.appliedComboIds.contains("COMBO-VP-SMISHING"))
    }

    @Test
    fun `scheme 없이 쓴 한글·영문 링크도 스미싱 조합에 링크로 센다`() {
        listOf(
            "택배 조회 대장방문.com/6ITtt 에서 확인",
            "택배 조회 택배조회.한국 에서 확인",
            "택배 조회 bit.ly/3xAb9 에서 확인",
            "택배 조회 대장방문[.]com/6ITtt 에서 확인"
        ).forEach { text ->
            val r = engine.analyze(text)
            assertTrue("[$text] ${r.appliedComboIds}", r.appliedComboIds.contains("COMBO-VP-SMISHING"))
        }
    }

    @Test
    fun `scheme 없이 쓴 공식 주소는 여전히 조합에서 뺀다`() {
        val r = engine.analyze("택배 조회는 www.cjlogistics.com 에서 운송장 번호로")
        assertFalse(r.appliedComboIds.contains("COMBO-VP-SMISHING"))
    }

}
