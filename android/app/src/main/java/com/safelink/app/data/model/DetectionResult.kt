package com.safelink.app.data.model

/**
 * 대화 감지 결과 (DetectionResultScreen 바인딩용)
 *
 * 근거 문서:
 * - 위험도 계산: docs/risk_scoring_v1.md (가중치/반복감쇠/조합보너스, 0~100점, 3단계: 낮음/중간/높음)
 * - 키워드 매칭: data/keyword.json (id, category, subcategory, weight, description)
 * - 기관 추천: data/institutions.json (risk_type_priority, subcategory_to_risk_type,
 *   standalone_recommend 게이팅, keyword_risk_type_additions)
 * - API 필드 대응: data/API 입출력 .json 의 response_example 참고
 *
 * 주의: score(0~100)는 항상 3단계(낮음/중간/높음) 기준으로 계산되고,
 * RiskLevel(SAFE/CAUTION/WARNING/CRITICAL) 4단계는 UI 표시 전용 변환값입니다.
 *   - SAFE      : score 0~15   (낮음 구간의 하위)
 *   - CAUTION   : score 16~30  (낮음 구간의 상위)
 *   - WARNING   : score 31~65  (중간 구간)
 *   - CRITICAL  : score 66~100 (높음 구간)
 * 변환 함수는 RiskLevel.fromScore(score) 참고.
 */
data class DetectionResult(
    val riskLevel: RiskLevel,
    val score: Int,                                  // 0~100
    val category: String,                             // "보이스피싱" | "로맨스스캠" | "가스라이팅"
    val originalText: String,                          // 사용자가 입력/붙여넣은 원문 (세션 내 표시만, 저장 안 함)
    val matchedKeywords: List<MatchedKeyword>,
    val recommendedInstitutions: List<RecommendedInstitutionUi>,
    val appliedComboIds: List<String> = emptyList(),   // 발동된 조합 보너스 규칙 id (설명용, 선택 표시)
    // 아래 2개는 2차 AI 보조 분석(DetectionRepository.escalateToAI)이 실행된 경우에만 채워짐.
    // 온디바이스 분석만 끝난 상태에서는 항상 null — 기존 더미데이터/화면 코드는 그대로 동작함.
    val aiSummary: String? = null,          // API 응답의 context_analysis_summary
    val aiDetectedPattern: String? = null,  // API 응답의 context_detected_pattern
    // 결과 화면에 "왜 이 점수가 나왔는지" 보여줄 근거 — 문장 규칙/상황 규칙 근거를 전용
    // 필드로 직접 분리했다(6주차, 김선한/신기훈 리뷰 반영: 태그로 섞인 단일 리스트는 화면에서
    // 매번 필터링해야 해서 "전용 구조"로 약하다는 지적). 키워드 근거는 matchedKeywords, AI
    // 근거는 aiSummary/aiDetectedPattern을 그대로 쓴다 — 이미 전용 필드가 있어서 안 만듦.
    // 기본값 emptyList()라 기존 더미데이터/화면 코드는 그대로 동작함. AnalysisEvidence.kt 참고.
    val sentenceRuleEvidences: List<AnalysisEvidence> = emptyList(),      // 문장 규칙 근거 (regex-complex, numeric_ratio_pattern)
    val situationalRuleEvidences: List<AnalysisEvidence> = emptyList()   // 상황 규칙 근거 (repeat_pattern, long_session_pattern)
) {
    /** 위험 없음(SAFE, 매칭 0건) 여부 — "위험한 표현이 감지되지 않았습니다" 문구 표시 조건 */
    val isSafeAndEmpty: Boolean get() = matchedKeywords.isEmpty()
}

data class MatchedKeyword(
    val keywordId: String,        // data/keyword.json 의 id (예: "VP-1-1-001")
    val subcategoryId: String,    // 예: "1-1"
    val subcategoryName: String,  // 예: "기관사칭" — 화면의 "감지된 위험 요소" 태그로 표시
    val matchedText: String,      // 원문에서 실제로 매칭된 문자열
    val startIndex: Int,          // 원문 내 시작 위치 (AnnotatedString 밑줄 강조용)
    val endIndex: Int,            // 원문 내 종료 위치 (exclusive)
    val weight: Int,              // data/keyword.json 의 weight 값 (근거 설명용)
    val description: String       // data/keyword.json 의 description (근거 텍스트로 재사용 가능)
)

data class RecommendedInstitutionUi(
    val institutionId: String,
    val name: String,
    val contact: String,
    val rank: Int,                // 1부터 중복 없이 순차 (같은 응답 안에서 유일해야 함)
    val reason: String,
    val matchedRiskType: String,  // "기관사칭" 등 institutions.json 7분류 값
    val group: String             // institutions.json의 group 필드 ("긴급대응"/"상담"/"공공지원"/"법률·행정지원")
                                   // UI에서는 Safe_Link_UI_Writing_Guide_v3.1.md 4장 기준으로
                                   // "긴급대응" -> 즉시 대응기관, 그 외 -> 추가 지원기관 으로 2단 표시
)
