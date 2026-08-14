package com.safelink.app.data.model

/**
 * 결과 화면에 "이 점수가 왜 나왔는지" 보여줄 근거 한 건 (5주차 신설, 6주차 구조 개선).
 * 어떤 종류의 근거인지는 [DetectionResult.sentenceRuleEvidences]/
 * [DetectionResult.situationalRuleEvidences] — 담기는 필드 자체가 곧 종류라서 이 클래스에는
 * 태그를 안 둔다 (필드로 직접 분리돼 있으니 화면에서 필터링할 필요가 없음).
 *
 * 키워드 근거는 기존 [DetectionResult.matchedKeywords], AI 근거는 기존
 * [DetectionResult.aiSummary]/[DetectionResult.aiDetectedPattern]를 그대로 쓴다 — 이미
 * 전용 필드가 있어서 새로 안 만듦. 신기훈 6주차 결과물 문서 참고.
 */
data class AnalysisEvidence(
    val label: String,   // 화면에 바로 쓸 수 있는 짧은 설명
    val detail: String   // 부가 설명 (매칭된 문구, 판정 조건 등 - keyword.json description/condition 재사용)
)
