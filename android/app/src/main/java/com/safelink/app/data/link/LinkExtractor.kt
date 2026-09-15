package com.safelink.app.data.link

/**
 * 원문에서 링크를 찾아내는 순수 Kotlin 유틸 (Android 의존성 없음 — 유닛 테스트 대상).
 *
 * 스미싱 문자·메신저 대화의 링크는 그냥 `https://...` 형태로만 오지 않는다.
 * 필터를 피하려고 일부러 망가뜨려 보내는 경우가 많아서, 아래를 모두 같은 링크로 본다.
 *
 * ```
 * https://evil.com/x     그대로
 * hxxps://evil.com/x     scheme 훼손
 * evil[.]com/x           점 훼손
 * evil(dot)com/x         점을 글자로
 * bit.ly/abcd            scheme 생략
 * 여기클릭bit.ly/abcd하세요   한글에 딱 붙은 링크
 * ```
 *
 * 훼손해서 보냈다는 사실 자체가 위험 신호이므로 [ExtractedLink.obfuscated] 로 남겨
 * 화면에서 따로 알려준다.
 *
 * 반대로 오탐(정상 문장을 링크로 착각)은 사용자를 지치게 하므로 보수적으로 잡는다.
 * scheme 이 없는 후보는 **알려진 TLD 로 끝날 때만** 링크로 인정한다.
 * 그래서 "버전 1.0", "설명.txt", "오전 9.30" 같은 건 걸리지 않는다.
 */
object LinkExtractor {

    /** 한 번의 분석에서 검사할 링크 최대 개수 — 링크 폭탄으로 검사가 늘어지지 않게 자른다. */
    const val MAX_LINKS = 10

    /**
     * scheme 없이 쓰인 후보를 링크로 인정할 최상위 도메인 목록.
     *
     * 전체 TLD(1,400여 개)를 넣으면 `.zip` `.mov` 같은 확장자와 겹쳐 오탐이 늘어난다.
     * 실제 국내 스미싱·피싱에서 관측되는 것 위주로 좁혀 두고, 사례가 쌓이면 추가한다.
     */
    private val KNOWN_TLDS = setOf(
        // 일반
        "com", "net", "org", "info", "biz", "mobi", "pro", "asia", "name",
        // 국가
        "kr", "jp", "cn", "us", "uk", "de", "fr", "ru", "in", "ph", "vn", "th", "hk", "tw",
        // 단축 URL 에서 자주 보이는 것
        "ly", "gl", "gd", "me", "to", "cc", "co", "tv", "ws", "io", "sh", "st", "im", "at", "be", "it",
        // 신규 gTLD — 스미싱에서 사용 빈도가 높은 것들
        "xyz", "top", "site", "online", "shop", "store", "click", "link", "live", "vip",
        "icu", "cyou", "sbs", "quest", "cfd", "bond", "rest", "fun", "life", "world",
        "today", "buzz", "digital", "email", "space", "tech", "art", "work", "app",
        "dev", "page", "cloud", "host", "website", "one", "run", "help", "support",
        // 한글 최상위 도메인 — "택배조회.한국" 처럼 scheme 없이 오면 목록에 없어 링크로 인정하지 못했다
        "한국", "닷컴", "닷넷"
    )

    /** `hxxp`, `h**p` 처럼 훼손된 scheme 까지 받아준다. */
    private const val SCHEME = """(?:h(?:tt|xx|\*\*)ps?)\s*[:：]\s*/\s*/\s*"""

    /** `.`, `[.]`, `(.)`, `[dot]`, `(dot)`, ` dot ` — 라벨 구분자로 쓰인 모든 형태. */
    private const val DOT = """(?:\s*(?:\[\s*(?:\.|dot)\s*\]|\(\s*(?:\.|dot)\s*\)|\{\s*\.\s*\}|\.)\s*)"""

    /** `[.]` `(dot)` 등 훼손된 구분자를 전부 `.` 하나로 되돌릴 때 쓴다. */
    private val DOT_SPLIT = Regex(DOT)

    /** 도메인 주소에 쓰일 수 있는 문자 — 링크 앞 경계를 판단하는 데 쓴다. */
    private val DOMAIN_CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('.', '-', '@', '_')

    /**
     * 라벨 하나는 둘 중 하나다.
     * 1) 영문/숫자 라벨: `bit`, `cjlogistics`
     * 2) 한글이 들어간 라벨: `대장방문`, `대한-통운`(하이픈), `대한통운24`(뒤에 숫자), `cj대한통운`(앞에 영문)
     *
     * 한글 라벨은 **한글이나 숫자로 끝나야 한다.** "여기클릭bit.ly"처럼 한글 단어 뒤에 영문 도메인이 dot 없이
     * 붙어 온 경우, 한글 라벨이 영문까지 삼켜 "여기클릭bit.ly"를 호스트로 잡으면 안 되기 때문이다.
     * 한글로 끝나야 하므로 "여기클릭"에서 라벨이 끊기고(뒤에 dot 이 없어 탈락), "bit.ly"만 링크가 된다.
     * 반대로 "대한통운24"처럼 한글 뒤에 **숫자만** 붙은 건 도메인 이름의 일부로 본다(예전엔 "24.com"만 잡혔다).
     */
    private const val LABEL =
        """(?:[A-Za-z0-9](?:[A-Za-z0-9\-]*[A-Za-z0-9])?|[A-Za-z0-9]*[가-힣](?:[가-힣A-Za-z0-9\-]*[가-힣0-9])?)"""

    /** 링크 뒤에 딸려 온 문장부호 — 경로의 일부가 아니므로 잘라낸다. */
    private const val TRAILING = """.,;:!?"'”’)]}>·…。、"""

    private val CANDIDATE = Regex(
        """($SCHEME)?($LABEL(?:$DOT$LABEL)+)(:\d{1,5})?((?:/|\?)[^\s가-힣ㄱ-ㅎㅏ-ㅣ<>"'“”]*)?""",
        RegexOption.IGNORE_CASE
    )

    /** 훼손 흔적 — 하나라도 있으면 "일부러 망가뜨린 링크"로 본다. */
    private val OBFUSCATION_MARK = Regex(
        """(?:hxx|h\*\*)|\[\s*(?:\.|dot)\s*\]|\(\s*(?:\.|dot)\s*\)|\{\s*\.\s*\}""",
        RegexOption.IGNORE_CASE
    )

    /**
     * [text] 안의 링크를 앞에서부터 순서대로 뽑는다.
     * 정규화된 URL 기준으로 중복을 제거하고 [MAX_LINKS] 개까지만 돌려준다.
     */
    fun extract(text: String): List<ExtractedLink> {
        if (text.isBlank()) return emptyList()

        val found = LinkedHashMap<String, ExtractedLink>()
        for (match in CANDIDATE.findAll(text)) {
            val link = toLink(match, text) ?: continue
            found.putIfAbsent(link.url, link)
            if (found.size >= MAX_LINKS) break
        }
        return found.values.toList()
    }

    private fun toLink(match: MatchResult, source: String): ExtractedLink? {
        // 앞 글자가 도메인에 쓰일 수 있는 문자거나 `@` 이면 (이메일 주소, 잘린 호스트) 링크가 아니다.
        // 한글은 도메인 문자가 아니므로 "여기클릭bit.ly/x" 처럼 딱 붙어 온 링크는 그대로 잡는다.
        val before = source.getOrNull(match.range.first - 1)
        if (before != null && before in DOMAIN_CHARS) return null

        val scheme = match.groupValues[1]
        val host = match.groupValues[2]
        val port = match.groupValues[3]
        val rawPath = match.groupValues[4]

        val hasScheme = scheme.isNotEmpty()
        val cleanHost = host.replace(DOT_SPLIT, ".").lowercase()
        val tld = cleanHost.substringAfterLast('.')

        // scheme 이 없으면 알려진 TLD 로 끝날 때만 링크로 본다 (오탐 방지)
        if (!hasScheme && tld !in KNOWN_TLDS) return null
        // scheme 이 있어도 TLD 자리가 숫자면 IP 주소 — 도메인 형태가 아니므로 여기선 제외
        if (tld.isEmpty() || tld.first().isDigit()) return null

        val path = rawPath.trimEnd { it in TRAILING }
        // 경로에 열지 않은 닫는 괄호가 붙어 온 경우 한 번 더 정리 (예: "(https://a.com/b)")
        val safePath = if (path.count { it == ')' } > path.count { it == '(' }) {
            path.substringBeforeLast(')')
        } else {
            path
        }

        val matchedText = source.substring(match.range.first, match.range.first + fullLength(match, safePath))
        val secure = scheme.contains("s", ignoreCase = true)
        val normalized = buildString {
            append(if (secure) "https://" else "http://")
            append(cleanHost)
            append(port)
            append(safePath)
        }

        return ExtractedLink(
            url = normalized,
            displayText = matchedText,
            obfuscated = OBFUSCATION_MARK.containsMatchIn(matchedText)
        )
    }

    /**
     * 한글 도메인을 검사 서버가 쓰는 ASCII(퓨니코드, `xn--...`) 형태로 바꾼다. 경로·쿼리는 그대로 둔다.
     * 위험 주소 목록은 호스트를 ASCII 로 정규화해 대조하므로, 한글 그대로 넘기면 목록에 있는 주소도 못 찾거나
     * "주소 형식을 확인할 수 없음"으로 끝날 수 있다. 영문 주소는 바뀌지 않는다. 변환에 실패하면 원래 주소를 돌려준다.
     */
    fun toAsciiUrl(url: String): String {
        val schemeEnd = url.indexOf("://").takeIf { it >= 0 }?.plus(3) ?: 0
        val rest = url.substring(schemeEnd)
        val hostEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' || it == ':' }.let { if (it < 0) rest.length else it }
        val host = rest.substring(0, hostEnd)
        if (host.all { it.code < 128 }) return url
        val ascii = runCatching { java.net.IDN.toASCII(host, java.net.IDN.ALLOW_UNASSIGNED) }.getOrNull() ?: return url
        return url.substring(0, schemeEnd) + ascii + rest.substring(hostEnd)
    }

    /** 뒤쪽 문장부호를 잘라낸 만큼 원문에서 잡아낼 길이도 줄인다. */
    private fun fullLength(match: MatchResult, trimmedPath: String): Int =
        match.value.length - (match.groupValues[4].length - trimmedPath.length)
}

/**
 * 원문에서 뽑아낸 링크 한 건.
 *
 * @property url 검사·표시에 쓰는 정규화된 주소 (scheme 보정, 훼손 복원, 소문자 호스트)
 * @property displayText 원문에 실제로 쓰여 있던 그대로 — "이 링크가 그 링크다"를 사용자가 확인할 수 있게 남긴다
 * @property obfuscated 필터 회피용으로 일부러 망가뜨려 쓴 흔적이 있는지 (`hxxp`, `evil[.]com` 등)
 */
data class ExtractedLink(
    val url: String,
    val displayText: String,
    val obfuscated: Boolean
)
