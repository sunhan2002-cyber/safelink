package com.safelink.app.ui.screens.support

/**
 * 기관별 신청 절차·필요 서류.
 *
 * 예전에는 모든 기관에 같은 안내("상담 예약 → 서류 준비 → 상담 → 결정")와 같은 서류 목록이 나왔다.
 * 경찰 신고와 법률구조 신청은 절차가 전혀 다른데도 같은 화면이 떠서, 안내로서 쓸모가 없었다.
 *
 * 여기 적은 것은 각 기관이 공개한 일반적인 접수 흐름이다. 사건 유형·지역에 따라 달라질 수 있으므로
 * 화면에도 "기관 사정에 따라 다를 수 있으니 전화로 먼저 확인하세요"를 함께 보여준다.
 * 목록에 없는 기관은 [DEFAULT] 를 쓴다.
 */
data class ApplicationGuide(
    val steps: List<String>,
    val documents: List<String>
)

object ApplicationGuideContent {

    fun forInstitution(institutionId: String): ApplicationGuide = GUIDES[institutionId] ?: DEFAULT

    private val DEFAULT = ApplicationGuide(
        steps = listOf(
            "대표번호로 전화해 상담 가능 여부와 준비물을 확인합니다",
            "아래 서류를 준비합니다",
            "전화 또는 방문으로 상담을 진행합니다",
            "안내받은 후속 절차를 진행합니다"
        ),
        documents = listOf("신분증", "피해 관련 증거 자료 (문자·대화 캡처)", "관련 계좌 거래 내역")
    )

    private val GUIDES = mapOf(
        // 경찰청 (112 / 사이버범죄 신고시스템 ECRM)
        "GOV-POLICE" to ApplicationGuide(
            steps = listOf(
                "112로 신고하거나 사이버범죄 신고시스템(ECRM)에 접수합니다",
                "송금한 경우, 같은 통화에서 계좌 지급정지를 함께 요청합니다",
                "대화·문자 캡처와 이체 내역을 제출합니다",
                "접수번호를 받아 두고 수사 진행 안내를 기다립니다"
            ),
            documents = listOf(
                "신분증",
                "대화·문자 원본 캡처",
                "이체 내역(은행 앱 화면 또는 거래확인증)",
                "상대방 계좌번호·전화번호 등 남아 있는 정보"
            )
        ),
        // 금융감독원 (1332) — 피해구제 신청
        "GOV-FSS" to ApplicationGuide(
            steps = listOf(
                "1332로 전화해 피해 내용을 상담합니다",
                "송금한 은행에 지급정지를 신청합니다(경찰 신고와 병행)",
                "피해구제 신청서를 작성해 해당 은행에 제출합니다",
                "채권소멸 절차와 환급 일정 안내를 받습니다"
            ),
            documents = listOf(
                "신분증",
                "피해구제 신청서(은행에 비치)",
                "경찰 신고 접수증 또는 사건사고사실확인원",
                "이체 내역"
            )
        ),
        // 한국인터넷진흥원 (118) — 스미싱·개인정보 침해
        "GOV-KISA" to ApplicationGuide(
            steps = listOf(
                "118로 전화해 받은 문자와 상황을 설명합니다",
                "스미싱 문자 원본을 안내에 따라 제출합니다",
                "휴대폰에 설치된 의심 앱을 점검하고 삭제합니다",
                "명의도용 여부 확인 방법을 안내받습니다"
            ),
            documents = listOf(
                "스미싱 문자 원본 캡처(발신번호가 보이게)",
                "설치된 의심 앱 이름",
                "피해가 있었다면 관련 거래 내역"
            )
        ),
        // 대한법률구조공단 (132)
        "PUB-LEGALAID" to ApplicationGuide(
            steps = listOf(
                "132로 전화해 상담을 예약합니다",
                "가까운 지부를 방문해 법률 상담을 받습니다",
                "필요하면 소송구조(무료 법률지원)를 신청합니다",
                "지원 대상 심사 결과와 이후 절차를 안내받습니다"
            ),
            documents = listOf(
                "신분증",
                "피해를 입증할 자료(대화·문자·이체 내역)",
                "소송구조 신청 시 소득을 확인할 수 있는 서류"
            )
        ),
        // 여성긴급전화 1366
        "PUB-WOMEN1366" to ApplicationGuide(
            steps = listOf(
                "1366으로 전화합니다(24시간, 서류 없이 상담 가능)",
                "상황을 설명하고 필요한 지원을 상담합니다",
                "필요하면 지역 센터·상담소로 연계받습니다",
                "보호·법률·의료 지원 중 필요한 절차를 안내받습니다"
            ),
            documents = listOf("따로 준비할 서류 없이 전화로 상담할 수 있습니다")
        )
    )
}
