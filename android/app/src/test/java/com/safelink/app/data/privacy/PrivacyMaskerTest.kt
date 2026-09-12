package com.safelink.app.data.privacy

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [PrivacyMasker] 검증 — 개인정보는 가리고, 판단에 필요한 값(금액·이름)은 남는가.
 */
class PrivacyMaskerTest {

    @Test
    fun `전화번호는 하이픈이 없어도 가린다`() {
        assertEquals("[전화번호]로 연락 주세요", PrivacyMasker.mask("010-1234-5678로 연락 주세요"))
        assertEquals("[전화번호]로 연락 주세요", PrivacyMasker.mask("01012345678로 연락 주세요"))
        assertEquals("[전화번호]로 연락 주세요", PrivacyMasker.mask("010 1234 5678로 연락 주세요"))
        assertEquals("[전화번호]로 연락 주세요", PrivacyMasker.mask("02-123-4567로 연락 주세요"))
    }

    @Test
    fun `링크는 훼손된 형태까지 잡고 경로만 가린다`() {
        // 도메인은 판단 근거라 남기고(A/B 검증 결과), 뒤의 경로·쿼리는 수신자 식별에 쓰일 수 있어 가린다
        assertEquals("여기 [링크: evil.example] 확인", PrivacyMasker.mask("여기 https://evil.example/login 확인"))
        assertEquals("여기 [링크: bit.ly] 확인", PrivacyMasker.mask("여기 bit.ly/abcd 확인"))
        assertEquals("여기 [링크: evil.kr] 확인", PrivacyMasker.mask("여기 www.evil.kr 확인"))
        assertEquals("여기 [링크: evil.example] 확인", PrivacyMasker.mask("여기 hxxp://evil.example 확인"))
        assertEquals("여기 [링크: evil.com] 확인", PrivacyMasker.mask("여기 evil[.]com/x 확인"))
    }

    @Test
    fun `계좌번호 주민등록번호 카드번호 이메일을 가린다`() {
        assertEquals("[계좌번호]로 입금", PrivacyMasker.mask("123456-78-901234로 입금"))
        assertEquals("[주민등록번호] 알려주세요", PrivacyMasker.mask("990101-1234567 알려주세요"))
        assertEquals("[카드번호] 입력", PrivacyMasker.mask("1234-5678-9012-3456 입력"))
        assertEquals("[이메일]로 보내주세요", PrivacyMasker.mask("hong@gmail.com로 보내주세요"))
    }

    @Test
    fun `판단에 필요한 금액과 이름은 남긴다`() {
        val text = "김민수 수사관입니다. 300만원을 오늘까지 보내세요"
        assertEquals(text, PrivacyMasker.mask(text))
    }

    @Test
    fun `날짜나 짧은 숫자 묶음은 계좌로 보지 않는다`() {
        assertEquals("2026-09-12 오후에 봐요", PrivacyMasker.mask("2026-09-12 오후에 봐요"))
        assertEquals("사건번호 12-34-56 확인", PrivacyMasker.mask("사건번호 12-34-56 확인"))
    }

    @Test
    fun `가린 자리에 표시가 남아 문맥은 그대로 읽힌다`() {
        val masked = PrivacyMasker.mask("검찰청입니다. 010-1234-5678로 전화하고 bit.ly/x 에서 확인하세요")
        assertEquals("검찰청입니다. [전화번호]로 전화하고 [링크: bit.ly] 에서 확인하세요", masked)
    }
}
