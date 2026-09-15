package com.safelink.app.data.link

/**
 * 공식 사이트로 확인된 도메인 — 문자에 이 주소만 있으면 "의심 링크"로 보지 않는다.
 *
 * ── 왜 필요한가 ──────────────────────────────────────────────────────
 * 규칙 엔진은 "택배 조회"·"부고장" 같은 스미싱 명목 문구와 링크가 함께 오면 가산점을 준다(COMBO-VP-SMISHING).
 * 그런데 "택배 조회는 https://www.cjlogistics.com 에서" 같은 **진짜 택배사 안내**도 똑같이 경고(45점)가 떴고,
 * 결과 화면에서 정상 주소가 "감지된 표현"으로 빨갛게 강조됐다. 정상 문자가 경고로 뜨면 앱 전체의 신뢰가 떨어진다.
 *
 * ── 무엇을 넣는가 ────────────────────────────────────────────────────
 * 사칭 대상이 되는 **택배사 공식 도메인**과 **정부 도메인(.go.kr)** 만 넣는다.
 * - `.go.kr` 은 정부·공공기관만 등록할 수 있는 영역이라 하위 주소 전체를 믿을 수 있다.
 * - 네이버·카카오처럼 **누구나 글·폼·블로그를 올릴 수 있는 도메인은 넣지 않는다** — 피싱 페이지가 그 안에 올라온다.
 * - 단축 주소(bit.ly 등)는 당연히 넣지 않는다.
 *
 * ── 흉내 낸 주소는 통과시키지 않는다 ─────────────────────────────────
 * 주소에서 실제 호스트만 떼어 **정확히 같거나, 점(.)으로 이어진 하위 도메인**일 때만 공식으로 본다.
 * - `cjlogistics.com.kr-track.top` → 호스트가 다르므로 아님
 * - `cjlogistics.com@evil.top` → @ 앞은 사용자 정보일 뿐 실제 호스트는 evil.top 이라 아님
 * - `cjlogistics-com.top`, `cj1ogistics.com` → 아님
 *
 * 이 목록은 규칙 점수에서만 쓴다. 링크 안전성 검사(Safe Browsing)는 공식 도메인이어도 그대로 한다.
 */
object OfficialDomains {

    /** 정확히 이 도메인이거나 그 하위 도메인이면 공식으로 본다. */
    private val DOMAINS = setOf(
        // 택배·물류
        "cjlogistics.com",   // CJ대한통운
        "epost.go.kr",       // 우체국택배 (.go.kr 에도 포함되지만 명시)
        "hanjin.com",        // 한진택배
        "hanjin.co.kr",
        "lotteglogis.com",   // 롯데택배
        "ilogen.com",        // 로젠택배
        "kdexp.com",         // 경동택배
        "cvsnet.co.kr",      // GS25 편의점택배
        "cupost.co.kr",      // CU 편의점택배
        "coupang.com",       // 쿠팡(로켓배송 조회)
        // 정부
        "gov.kr",            // 정부24 — go.kr 과 다른 도메인이라 따로 둔다
    )

    /** 이 접미사로 끝나는 호스트는 공식으로 본다(등록 자격이 제한된 영역). */
    private val SUFFIXES = setOf(
        "go.kr",             // 정부기관
    )

    fun isOfficialUrl(url: String): Boolean {
        val host = hostOf(url) ?: return false
        return DOMAINS.any { host == it || host.endsWith(".$it") } ||
            SUFFIXES.any { host == it || host.endsWith(".$it") }
    }

    /**
     * 주소에서 실제 호스트만 뗀다. 한글·공백이 섞인 주소도 있어 URI 파서 대신 직접 자른다.
     * scheme 제거 → 경로·쿼리·조각 앞까지 → 사용자 정보(@ 앞) 제거 → 포트 제거 → 소문자·끝 점 제거.
     */
    internal fun hostOf(url: String): String? {
        val withoutScheme = url.trim().substringAfter("://", url.trim())
        val authority = withoutScheme.takeWhile { it != '/' && it != '?' && it != '#' }
        val hostPort = authority.substringAfterLast('@')
        val host = hostPort.substringBefore(':').lowercase().trimEnd('.')
        return host.takeIf { it.isNotEmpty() && '.' in it }
    }
}
