package com.safelink.app.data.model

/**
 * DetectionResultScreen 개발/미리보기용 더미 데이터.
 *
 * 3개 카테고리 x 서로 다른 위험도 구간을 커버하도록 구성 (docs/test_sentences_v1.json 의
 * 검증된 테스트 케이스를 재사용 — 점수가 실제 계산 로직과 일치함을 보장).
 *
 *  - vpCritical : 보이스피싱, 82.5점 → CRITICAL (test_sentences_v1.json TC-VP-EDGE-02 재사용)
 *  - rsWarning  : 로맨스스캠, 47.5점 → WARNING  (TC-RS-EDGE-01 재사용, 추천기관 3개 병합·rank 재정렬 예시)
 *  - glCaution  : 가스라이팅, 24점  → CAUTION  (추천기관 없음 — standalone_recommend 게이팅 정상 동작 예시)
 */
object DetectionResultDummyData {

    val vpCritical = DetectionResult(
        riskLevel = RiskLevel.CRITICAL,
        score = 82,
        category = "보이스피싱",
        originalText = "택배기사인데요, 배송 중 확인 차 연락드렸습니다. 그럼 명의 도용 우려가 있어서 " +
            "확인이 필요합니다. 지금 당장 확인 안 하시면 계좌가 압류될 수 있습니다.",
        matchedKeywords = listOf(
            MatchedKeyword("VP-1-1-001", "1-1", "기관사칭", "택배기사", 0, 4, 15, "택배기사 사칭 접근"),
            MatchedKeyword("VP-1-1-002", "1-1", "기관사칭", "배송 중", 9, 13, 15, "택배기사 사칭 접근"),
            MatchedKeyword("VP-1-2-002", "1-2", "명의도용/사건연루 경고", "명의 도용 우려", 31, 39, 15, "명의도용 우려 고지"),
            MatchedKeyword("VP-1-4-001", "1-4", "긴급성 압박/법적 위협", "지금 당장", 56, 61, 20, "긴급성 압박"),
            MatchedKeyword("VP-1-4-004", "1-4", "긴급성 압박/법적 위협", "압류", 75, 77, 20, "법적 위협")
        ),
        recommendedInstitutions = listOf(
            RecommendedInstitutionUi("GOV-POLICE", "경찰청 (사이버수사대)", "112", 1, "기관사칭 및 범죄 신고, 수사 요청 가능", "기관사칭", "긴급대응"),
            RecommendedInstitutionUi("GOV-FSS", "금융감독원 (금융사기대응단)", "1332", 2, "기관사칭 및 금융사기 가능성에 대한 피해 상담과 지급정지 안내 가능", "기관사칭", "긴급대응"),
            RecommendedInstitutionUi("GOV-PIPC", "개인정보보호위원회", "118", 3, "명의도용 및 개인정보 탈취 우려에 대한 신고 가능", "개인정보탈취", "긴급대응")
        ),
        appliedComboIds = listOf("COMBO-GENERAL-3CAT")
    )

    val rsWarning = DetectionResult(
        riskLevel = RiskLevel.WARNING,
        score = 47,
        category = "로맨스스캠",
        originalText = "지방 파견 나와 있어요, 무역회사 대표입니다. 진심으로 마음이 가요, 당신 생각을 " +
            "안 할 수가 없어요. 이건 아무한테도 말하지 말아주세요, 우리 둘만 아는 걸로 해요.",
        matchedKeywords = listOf(
            MatchedKeyword("RS-2-1-001", "2-1", "신원 설정", "지방 파견", 0, 5, 5, "그럴듯한 국내형 직업 설정"),
            MatchedKeyword("RS-2-1-002", "2-1", "신원 설정", "무역회사 대표", 14, 21, 5, "직업 설정"),
            MatchedKeyword("RS-2-2-002", "2-2", "급속 친밀감 형성", "진심으로 마음이 가요", 26, 37, 10, "급속 애정 표현"),
            MatchedKeyword("RS-2-3-002", "2-3", "비밀유지요구/고립유도", "아무한테도 말하지 말아주세요", 61, 76, 15, "비밀 유지 요구")
        ),
        // 2-1/2-2/2-3 모두 단독으로는 standalone_recommend=false 이지만, 3개 중분류가 동시
        // 감지되어 조합보너스(COMBO-GENERAL-3CAT)가 발동 + 세션 점수가 중간(31점 이상)이라
        // 추천 조건을 충족함 — 여러 위험유형이 하나의 배열로 병합되고 rank가 1부터 재정렬된 예시
        recommendedInstitutions = listOf(
            RecommendedInstitutionUi("GOV-FSS", "금융감독원 (금융사기대응단)", "1332", 1, "금융사기 피해 상담 및 지급정지 가능", "금융사기", "긴급대응"),
            RecommendedInstitutionUi("PUB-MENTALHEALTH", "한국심리학회·지역 정신건강복지센터", "지역별 센터", 2, "심리적 조작·가스라이팅 전문 상담 및 정신건강 지원", "심리조작", "상담"),
            RecommendedInstitutionUi("PUB-WOMEN1366", "여성가족부·한국여성인권진흥원", "1366", 3, "관계 내 조작·통제 피해자 상담 및 보호", "심리조작", "상담")
        ),
        appliedComboIds = listOf("COMBO-GENERAL-3CAT")
    )

    val glCaution = DetectionResult(
        riskLevel = RiskLevel.CAUTION,
        score = 24,
        category = "가스라이팅",
        originalText = "내가 언제 그렇게 말했어? 너는 항상 왜곡해서 기억하더라. 다 너를 위해서 하는 말이야, 진심이야.",
        matchedKeywords = listOf(
            MatchedKeyword("GL-3-2-001", "3-2", "현실 왜곡", "내가 언제 그렇게 말했어", 0, 13, 12, "기억 부정"),
            MatchedKeyword("GL-3-4-001", "3-4", "죄책감 유도", "다 너를 위해서 하는 말이야", 33, 48, 12, "죄책감 유도")
        ),
        // 3-2, 3-4 둘 다 standalone_recommend=false 이고, 서로 다른 중분류 2개뿐이라 일반
        // 조합보너스(3개 이상 필요)도 발동 안 함 + 세션 점수 24점은 아직 "낮음" 구간 →
        // 추천 기관 없음(빈 리스트)이 정상 동작. UI에서 "추천 기관 없음" 상태 테스트용.
        recommendedInstitutions = emptyList(),
        appliedComboIds = emptyList()
    )

    /** 위험 없음(SAFE) 상태 — "위험한 표현이 감지되지 않았습니다" 문구 테스트용 */
    val safeEmpty = DetectionResult(
        riskLevel = RiskLevel.SAFE,
        score = 0,
        category = "",
        originalText = "오늘 저녁에 뭐 먹을까? 나는 파스타 먹고 싶어.",
        matchedKeywords = emptyList(),
        recommendedInstitutions = emptyList()
    )

    val all = listOf(vpCritical, rsWarning, glCaution, safeEmpty)
}
