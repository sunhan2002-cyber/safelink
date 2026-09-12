package com.safelink.app.data.privacy

import com.safelink.app.data.link.LinkExtractor

/**
 * AI 보조분석으로 내보내기 전에 대화에서 개인정보를 가린다.
 *
 * ── 무엇을 가리고, 무엇을 남기는가 ──────────────────────────────────────
 * 가리는 것: 전화번호, 링크의 경로·쿼리, 계좌번호, 주민등록번호, 카드번호, 이메일.
 * 이것들은 **누구인지·어디로 보내는지**를 특정하는 값이라 판단에 필요하지 않다.
 * 자리에는 `[전화번호]` 같은 표시를 남겨서 "그 자리에 무엇이 있었는지"는 그대로 읽히게 한다
 * (예: "[전화번호]로 바로 연락 주세요" 는 여전히 제3자 연결 유도로 읽힌다).
 *
 * 남기는 것: **금액과 이름**.
 * - 금액: "300만원"이 소액인지 거액인지가 판단의 핵심이다. 가리면 AI 가 볼 근거가 사라진다.
 * - 이름: "검찰청 김민수 수사관입니다" 처럼 기관·가족 사칭 판단에 쓰인다.
 *
 * ── 왜 링크는 추출기와 같은 기준을 쓰는가 ──────────────────────────────
 * 예전 마스킹은 `https?://` 로 시작하는 링크만 가렸다. 그런데 실제 스미싱은 `bit.ly/x`, `www.evil.kr`,
 * `hxxp://`, `evil[.]com` 처럼 보내는 경우가 더 많다. 링크 검사([LinkExtractor])는 이미 그 형태를 전부
 * 잡고 있으므로, 마스킹도 같은 추출기를 써서 검사와 기준을 맞춘다.
 *
 * ── 온디바이스 판정에는 영향이 없다 ────────────────────────────────────
 * 규칙 엔진은 항상 원문을 본다. 이 함수는 네트워크로 나가는 사본에만 적용된다.
 */
object PrivacyMasker {

    /** 이메일 — 링크보다 먼저 가린다(도메인이 링크로 잡히지 않도록). */
    private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    /** 주민등록번호 — 계좌번호보다 먼저 본다(형태가 겹친다). */
    private val RESIDENT_NUMBER = Regex("""(?<![0-9])\d{6}-[1-4]\d{6}(?![0-9])""")

    /** 카드번호 4-4-4-4 */
    private val CARD_NUMBER = Regex("""(?<![0-9])\d{4}[- ]\d{4}[- ]\d{4}[- ]\d{4}(?![0-9])""")

    /**
     * 전화번호 — 하이픈이 없거나 공백으로 띄운 형태까지 본다.
     * 예전에는 `\d{2,3}-\d{3,4}-\d{4}` 뿐이라 문자로 흔히 오는 `01012345678` 이 그대로 나갔다.
     */
    private val PHONE = Regex(
        """(?<![0-9])(?:01[016-9][-\s.]?\d{3,4}[-\s.]?\d{4}|0\d{1,2}[-\s]\d{3,4}[-\s]\d{4}|1[5-9]\d{2}[-\s]?\d{4})(?![0-9])"""
    )

    /**
     * 계좌번호 — 숫자 묶음 세 개.
     * 날짜(2026-09-12)처럼 같은 모양인데 계좌가 아닌 값을 가리지 않도록, 숫자 자릿수가
     * [MIN_ACCOUNT_DIGITS] 이상일 때만 계좌로 본다.
     */
    private val ACCOUNT_NUMBER = Regex("""(?<![0-9])\d{2,6}-\d{2,6}-\d{2,8}(?![0-9])""")

    private const val MIN_ACCOUNT_DIGITS = 10

    /** 정규화된 주소에서 도메인만 뽑는다. 못 뽑으면 "주소 미상". */
    private fun hostOf(url: String): String =
        HOST.find(url)?.groupValues?.get(1)?.lowercase()?.removePrefix("www.") ?: "주소 미상"

    private val HOST = Regex("""^[A-Za-z]+://([^/:?#]+)""")

    fun mask(text: String): String {
        if (text.isBlank()) return text
        var masked = EMAIL.replace(text, "[이메일]")

        // 링크는 검사와 같은 추출기로 찾아 원문에 쓰인 그대로를 치환하되, **도메인은 남긴다**.
        // 주소의 경로·쿼리에는 수신자를 식별하는 값이 붙어 오는 경우가 있어 가리고,
        // 도메인("funeral-notice.top")은 그 자체가 위험 판단의 근거라 남긴다 —
        // A/B 검증에서 도메인까지 가렸더니 스미싱 문자에 대한 AI 보정이 +28 에서 +8 로 떨어졌다.
        LinkExtractor.extract(masked).forEach { link ->
            if (link.displayText.isNotBlank()) {
                masked = masked.replace(link.displayText, "[링크: " + hostOf(link.url) + "]")
            }
        }

        masked = RESIDENT_NUMBER.replace(masked, "[주민등록번호]")
        masked = CARD_NUMBER.replace(masked, "[카드번호]")
        masked = PHONE.replace(masked, "[전화번호]")
        masked = ACCOUNT_NUMBER.replace(masked) { match ->
            if (match.value.count(Char::isDigit) >= MIN_ACCOUNT_DIGITS) "[계좌번호]" else match.value
        }
        return masked
    }
}
