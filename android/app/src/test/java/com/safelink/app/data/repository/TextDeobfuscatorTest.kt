package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** 글자 사이에 기호·보이지 않는 문자를 끼워 규칙을 피하는 표현 */
class TextDeobfuscatorTest {

    companion object {
        private lateinit var engine: DetectionEngine

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val kw = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
            val inst = javaClass.classLoader!!.getResourceAsStream("institutions.json")!!.bufferedReader().readText()
            engine = DetectionEngine(
                gson.fromJson(kw, KeywordData::class.java),
                gson.fromJson(inst, InstitutionData::class.java),
                gson
            )
        }
    }

    @Test
    fun `한글 사이 기호와 보이지 않는 문자를 지운다`() {
        assertEquals("송금해주세요", TextDeobfuscator.clean("송.금해주세요").text)
        assertEquals("인증번호", TextDeobfuscator.clean("인_증_번_호").text)
        assertEquals("입금", TextDeobfuscator.clean("입**금").text)
        assertEquals("송금", TextDeobfuscator.clean("송\u200B금").text)
        assertEquals("송금", TextDeobfuscator.clean("송\u200B.금").text)
    }

    @Test
    fun `링크와 보통 문장 부호는 건드리지 않는다`() {
        listOf(
            "대장방문.com/6ITtt",
            "evil[.]com 들어가",
            "택배 조회 택배조회.한국 에서 확인",
            "응. 알겠어",
            "진짜?거짓말",
            "대박!최고",
            "좋아~내일봐",
            "3.5만원",
            "ㅋㅋ ㅎㅎ"
        ).forEach { assertFalse(it, TextDeobfuscator.clean(it).changed) }
    }

    @Test
    fun `기호를 끼운 사기 문장도 기호 없는 문장과 같은 점수를 받는다`() {
        val plain = "엄마 나 폰 고장나서 그런데 급하게 송금해 줄 수 있어? 계좌 알려줄게 인증번호도 알려줘"
        val obfuscated = "엄마 나 폰 고.장나서 그런데 급하게 송.금해 줄 수 있어? 계*좌 알려줄게 인_증_번_호도 알\u200B려줘"
        val a = engine.analyze(plain)
        val b = engine.analyze(obfuscated)
        assertEquals(a.score, b.score)
        assertEquals(a.matchedKeywords.map { it.keywordId }, b.matchedKeywords.map { it.keywordId })
        assertTrue("경고 이상: ${b.score}", b.score >= 31)
    }

    @Test
    fun `밑줄 위치는 원문 기준으로 되돌린다`() {
        val text = "지금\n송.금 해주세요"
        val result = engine.analyze(text)
        assertEquals(text, result.originalText)
        assertTrue(result.matchedKeywords.isNotEmpty())
        result.matchedKeywords.forEach { k ->
            assertEquals(k.matchedText, text.substring(k.startIndex, k.endIndex))
        }
        assertTrue(result.matchedKeywords.any { it.matchedText.contains("송.금") })
    }

    @Test
    fun `여러 턴에서도 위치가 원문과 맞는다`() {
        val turns = listOf("엄마 나야 폰 고.장났어", "인_증_번_호 알려줘")
        val result = engine.analyze(turns)
        assertEquals(turns.joinToString("\n"), result.originalText)
        result.matchedKeywords.forEach { k ->
            assertEquals(k.matchedText, result.originalText.substring(k.startIndex, k.endIndex))
        }
    }
}
