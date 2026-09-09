package com.safelink.app.data.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [LinkExtractor] 검증.
 *
 * 실제 스미싱 문구에서 링크가 어떤 모양으로 오는지를 기준으로 삼았다.
 * 핵심은 두 방향이다 — 훼손해서 보낸 링크도 놓치지 않을 것, 정상 문장을 링크로 착각하지 말 것.
 */
class LinkExtractorTest {

    private fun urls(text: String) = LinkExtractor.extract(text).map { it.url }

    // --- 기본 형태 -------------------------------------------------------

    @Test
    fun `평범한 https 링크를 뽑는다`() {
        assertEquals(listOf("https://safelink.example.com/a"), urls("확인은 https://safelink.example.com/a 에서"))
    }

    @Test
    fun `scheme 이 없어도 알려진 TLD 면 링크로 본다`() {
        assertEquals(listOf("http://bit.ly/3xAb9"), urls("여기서 확인 bit.ly/3xAb9"))
    }

    @Test
    fun `한글에 딱 붙어 있어도 링크만 잘라낸다`() {
        assertEquals(listOf("http://bit.ly/3xAb9"), urls("여기클릭bit.ly/3xAb9하세요"))
    }

    @Test
    fun `문장 끝 마침표와 괄호는 링크에 포함하지 않는다`() {
        assertEquals(listOf("https://a.example.com/b"), urls("주소는 (https://a.example.com/b) 입니다."))
    }

    @Test
    fun `포트가 붙은 주소도 유지한다`() {
        assertEquals(listOf("http://a.example.com:8080/x"), urls("접속: http://a.example.com:8080/x"))
    }

    // --- 훼손된 링크 -----------------------------------------------------

    @Test
    fun `hxxp 로 훼손한 scheme 을 되돌린다`() {
        val link = LinkExtractor.extract("hxxps://evil.example.com/pay").single()
        assertEquals("https://evil.example.com/pay", link.url)
        assertTrue(link.obfuscated)
    }

    @Test
    fun `대괄호로 훼손한 점을 되돌린다`() {
        val link = LinkExtractor.extract("접속 evil[.]example[.]com/login 하세요").single()
        assertEquals("http://evil.example.com/login", link.url)
        assertTrue(link.obfuscated)
    }

    @Test
    fun `dot 을 글자로 쓴 것도 되돌린다`() {
        assertEquals(listOf("http://evil.example.com"), urls("evil(dot)example(dot)com 으로"))
    }

    @Test
    fun `정상 링크는 훼손으로 표시하지 않는다`() {
        assertFalse(LinkExtractor.extract("https://naver.com").single().obfuscated)
    }

    // --- 오탐 방지 -------------------------------------------------------

    @Test
    fun `버전 번호나 파일 이름은 링크가 아니다`() {
        assertTrue(urls("버전 1.0 으로 올렸고 설명.txt 파일 첨부").isEmpty())
        assertTrue(urls("오전 9.30 에 만나요").isEmpty())
    }

    @Test
    fun `이메일 주소는 링크로 잡지 않는다`() {
        assertTrue(urls("문의는 help@example.com 으로 주세요").isEmpty())
    }

    @Test
    fun `모르는 TLD 는 scheme 이 없으면 링크로 보지 않는다`() {
        assertTrue(urls("파일명은 report.hwpx 입니다").isEmpty())
    }

    @Test
    fun `링크가 없으면 빈 목록`() {
        assertTrue(urls("안녕하세요 오늘 시간 되세요?").isEmpty())
        assertTrue(urls("").isEmpty())
    }

    // --- 목록 처리 -------------------------------------------------------

    @Test
    fun `같은 링크가 여러 번 나와도 한 건으로 센다`() {
        assertEquals(
            listOf("http://bit.ly/aa"),
            urls("bit.ly/aa 확인하고 다시 bit.ly/aa 눌러주세요")
        )
    }

    @Test
    fun `호스트 대소문자가 달라도 같은 링크로 본다`() {
        assertEquals(listOf("http://bit.ly/aa"), urls("BIT.LY/aa 와 bit.ly/aa"))
    }

    @Test
    fun `링크가 아주 많아도 상한까지만 검사한다`() {
        val text = (1..30).joinToString(" ") { "bit.ly/$it" }
        assertEquals(LinkExtractor.MAX_LINKS, LinkExtractor.extract(text).size)
    }

    @Test
    fun `원문에 쓰인 그대로도 함께 남긴다`() {
        val link = LinkExtractor.extract("지금 evil[.]example[.]com 접속").single()
        assertEquals("evil[.]example[.]com", link.displayText)
    }

    // --- 실제 스미싱 문구 -------------------------------------------------

    @Test
    fun `택배 사칭 스미싱 문구에서 링크를 뽑는다`() {
        val text = "[Web발신] 고객님 주소불일치로 택배가 반송되었습니다 주소지 확인 hxxp://kr-post[.]top/chk"
        val link = LinkExtractor.extract(text).single()
        assertEquals("http://kr-post.top/chk", link.url)
        assertTrue(link.obfuscated)
    }
}
