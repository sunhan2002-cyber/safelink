package com.safelink.app.data.link

/** 링크 한 건에 대한 안전성 검사 결과. */
data class LinkRiskResult(
    val link: ExtractedLink,
    val verdict: LinkVerdict,
    val threat: LinkThreat? = null,
    /** [LinkVerdict.UNCHECKED] 일 때 사용자에게 보여줄 사유 (그 외에는 null) */
    val uncheckedReason: String? = null
)

enum class LinkVerdict {
    /** 구글 차단 목록에 등재된 주소 — 절대 열지 말 것 */
    DANGEROUS,

    /**
     * 차단 목록에 없음.
     *
     * "안전하다"가 아니라 **"아직 신고·등재되지 않았다"** 는 뜻이다.
     * 새로 만든 스미싱 도메인은 등재까지 시간이 걸리므로, 화면 문구도 "안전함"이 아니라
     * "알려진 위험 목록에는 없음"으로 쓴다.
     */
    NO_MATCH,

    /** 검사하지 못함 — API 키 미설정, 차단 목록 미준비, 네트워크 없음, 주소 형식 오류 등 */
    UNCHECKED
}

/** 구글 Safe Browsing 위협 분류를 앱 문구로 옮긴 것. */
enum class LinkThreat(val label: String, val description: String) {
    SOCIAL_ENGINEERING(
        "피싱·사칭 사이트",
        "기관이나 지인을 사칭해 계정·금융정보를 가로채는 것으로 신고된 주소입니다."
    ),
    MALWARE(
        "악성코드 유포",
        "접속만으로 기기를 감염시키는 악성코드가 확인된 주소입니다."
    ),
    UNWANTED_SOFTWARE(
        "유해 프로그램",
        "사용자가 원하지 않는 프로그램을 설치하도록 유도하는 주소입니다."
    ),
    HARMFUL_APP(
        "악성 앱 설치 유도",
        "기기를 원격 조종하거나 정보를 빼내는 앱을 설치하도록 유도하는 주소입니다."
    )
}
