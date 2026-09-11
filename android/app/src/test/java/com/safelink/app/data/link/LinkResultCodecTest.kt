package com.safelink.app.data.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [LinkResultCodec] 검증 — 기록에 남긴 링크 판정이 다시 열 때 사라지거나 약해지지 않는가. */
class LinkResultCodecTest {

    private fun result(url: String, verdict: LinkVerdict, threat: LinkThreat? = null) =
        LinkRiskResult(ExtractedLink(url, "$url 원문", obfuscated = false), verdict, threat)

    private val phishing = result("https://evil.example/login", LinkVerdict.DANGEROUS, LinkThreat.SOCIAL_ENGINEERING)
    private val clean = result("https://news.example/a", LinkVerdict.NO_MATCH)

    @Test
    fun `저장한 판정을 그대로 읽어온다`() {
        val decoded = LinkResultCodec.decode(LinkResultCodec.encode(listOf(phishing, clean)))
        assertEquals(listOf(phishing, clean), decoded)
    }

    @Test
    fun `검사하지 못한 건은 저장하지 않는다`() {
        assertNull(LinkResultCodec.encode(listOf(result("https://a.example", LinkVerdict.UNCHECKED))))
        val decoded = LinkResultCodec.decode(LinkResultCodec.encode(listOf(phishing, result("https://b.example", LinkVerdict.UNCHECKED))))
        assertEquals(listOf(phishing), decoded)
    }

    @Test
    fun `비었거나 깨진 값은 빈 목록`() {
        assertTrue(LinkResultCodec.decode(null).isEmpty())
        assertTrue(LinkResultCodec.decode("").isEmpty())
        assertTrue(LinkResultCodec.decode("이건 JSON 이 아니다").isEmpty())
        assertTrue(LinkResultCodec.decode("""[{"url":"https://a.example","verdict":"없는값"}, null]""").isEmpty())
    }

    @Test
    fun `당시 위험했던 주소는 새 검사에서 목록에 없어도 위험으로 남는다`() {
        val merged = LinkResultCodec.merge(listOf(phishing), listOf(phishing.copy(verdict = LinkVerdict.NO_MATCH, threat = null)))
        assertEquals(listOf(phishing), merged)
    }

    @Test
    fun `새 검사가 실패하면 저장된 판정을 쓴다`() {
        val merged = LinkResultCodec.merge(listOf(clean), listOf(clean.copy(verdict = LinkVerdict.UNCHECKED)))
        assertEquals(listOf(clean), merged)
    }

    @Test
    fun `나중에 위험으로 등재된 주소는 새 결과를 쓴다`() {
        val nowDangerous = clean.copy(verdict = LinkVerdict.DANGEROUS, threat = LinkThreat.MALWARE)
        assertEquals(listOf(nowDangerous), LinkResultCodec.merge(listOf(clean), listOf(nowDangerous)))
    }

    @Test
    fun `새 검사 목록이 비어도 저장된 판정은 버리지 않는다`() {
        assertEquals(listOf(phishing), LinkResultCodec.merge(listOf(phishing), emptyList()))
    }
}
