package com.safelink.app.data.model

/**
 * 결과 화면에 "이 점수가 왜 나왔는지" 보여줄 근거 한 건 (5주차 신설).
 * [DetectionEngine][com.safelink.app.data.repository.DetectionEngine]의 1차 분석 3개 층
 * (키워드/문장 규칙/상황 규칙) + 2차 AI 보조 분석까지, 근거를 [source] 기준으로 구분해서
 * 노출한다 — 김우영이 "문장 규칙 근거 / 상황 규칙 근거 / AI 보조분석 근거"를 구분해서
 * 화면 문구를 정리할 수 있도록 하기 위함. 신기훈 5주차 결과물 03번 문서 참고.
 *
 * 분류 기준 (신기훈 5주차 01번 문서 "완료 기준 검증" 예시와 동일한 기준):
 * - 키워드: `keyword`/`regex-simple` 매칭, 여러 키워드 co-occurrence 콤보(예: COMBO-GENERAL-3CAT)도 포함
 * - 문장 규칙: `regex-complex`(숫자 임계값 조건) 매칭, `numeric_ratio_pattern` 콤보(숫자 관계 계산)
 * - 상황 규칙: `repeat_pattern`/`long_session_pattern` 콤보(세션 전체 반복/장기세션)
 * - AI 보조분석: `mergeAiResponse()`로 병합된 서버 응답
 */
data class AnalysisEvidence(
    val source: String,  // SOURCE_KEYWORD | SOURCE_SENTENCE_RULE | SOURCE_SITUATIONAL_RULE | SOURCE_AI
    val label: String,   // 화면에 바로 쓸 수 있는 짧은 설명
    val detail: String   // 부가 설명 (매칭된 문구, 판정 조건 등 - keyword.json description/condition 재사용)
) {
    companion object {
        const val SOURCE_KEYWORD = "키워드"
        const val SOURCE_SENTENCE_RULE = "문장 규칙"
        const val SOURCE_SITUATIONAL_RULE = "상황 규칙"
        const val SOURCE_AI = "AI 보조분석"
    }
}
