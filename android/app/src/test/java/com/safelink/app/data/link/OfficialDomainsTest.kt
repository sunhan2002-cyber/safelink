package com.safelink.app.data.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialDomainsTest {

    @Test
    fun `택배사·정부 공식 주소와 그 하위 도메인은 공식으로 본다`() {
        assertTrue(OfficialDomains.isOfficialUrl("https://www.cjlogistics.com"))
        assertTrue(OfficialDomains.isOfficialUrl("https://www.cjlogistics.com/ko/tool/parcel/tracking?no=123"))
        assertTrue(OfficialDomains.isOfficialUrl("http://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm"))
        assertTrue(OfficialDomains.isOfficialUrl("https://www.gov.kr/portal/main"))
        assertTrue(OfficialDomains.isOfficialUrl("HTTPS://WWW.HANJIN.COM:443/kor"))
    }

    @Test
    fun `공식 주소를 흉내 낸 주소는 공식이 아니다`() {
        assertFalse("다른 도메인 앞에 붙임", OfficialDomains.isOfficialUrl("https://cjlogistics.com.kr-track.top/a"))
        assertFalse("@ 앞은 사용자 정보일 뿐", OfficialDomains.isOfficialUrl("https://cjlogistics.com@evil.top/a"))
        assertFalse("하이픈으로 이어 붙임", OfficialDomains.isOfficialUrl("http://cjlogistics-com.top/k2"))
        assertFalse("글자 바꿔치기", OfficialDomains.isOfficialUrl("http://cj1ogistics.com/track"))
        assertFalse("go.kr 흉내", OfficialDomains.isOfficialUrl("http://gov-kr.site/refund"))
        assertFalse("단축 주소", OfficialDomains.isOfficialUrl("http://bit.ly/xyz123"))
        assertFalse("누구나 글을 올리는 포털은 넣지 않음", OfficialDomains.isOfficialUrl("https://blog.naver.com/abc"))
    }

    @Test
    fun `호스트만 정확히 떼어낸다`() {
        assertEquals("evil.top", OfficialDomains.hostOf("https://cjlogistics.com@evil.top:8080/x?y=1#z"))
        assertEquals("www.cjlogistics.com", OfficialDomains.hostOf("https://www.cjlogistics.com."))
    }
}
