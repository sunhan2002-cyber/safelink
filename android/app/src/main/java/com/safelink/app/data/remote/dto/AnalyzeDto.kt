package com.safelink.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * data/API 입출력 .json 스키마 그대로 매핑한 DTO. 서버(목/실서버 공통) 요청·응답 형태.
 * 이 파일 자체가 API 계약이라 필드명은 JSON 원본 키(snake_case)를 @SerializedName으로
 * 그대로 유지한다.
 */
data class AnalyzeRequestDto(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("masked_text") val maskedText: String,
    @SerializedName("recent_turns") val recentTurns: List<String>,
    @SerializedName("device_base_score") val deviceBaseScore: Double,
    @SerializedName("device_matched_ids") val deviceMatchedIds: List<String>,
    @SerializedName("device_applied_combo_ids") val deviceAppliedComboIds: List<String> = emptyList(),
    @SerializedName("category_hint") val categoryHint: String? = null
)

data class AnalyzeResponseDto(
    @SerializedName("context_score_adjustment") val contextScoreAdjustment: Double,
    @SerializedName("context_analysis_summary") val contextAnalysisSummary: String,
    @SerializedName("context_detected_pattern") val contextDetectedPattern: String?,
    @SerializedName("recommended_level_override") val recommendedLevelOverride: String?, // "낮음"|"중간"|"높음"|null
    @SerializedName("guide_reference_id") val guideReferenceId: String? = null,          // 하위호환용, 항상 null
    @SerializedName("matched_keyword_ids") val matchedKeywordIds: List<String>,
    @SerializedName("recommended_institutions") val recommendedInstitutions: List<RecommendedInstitutionDto>,
    @SerializedName("analysis_timestamp") val analysisTimestamp: String
)

data class RecommendedInstitutionDto(
    @SerializedName("institution_id") val institutionId: String,
    @SerializedName("rank") val rank: Int,
    @SerializedName("reason") val reason: String,
    @SerializedName("matched_risk_type") val matchedRiskType: String,
    @SerializedName("matched_subcategory_id") val matchedSubcategoryId: String
)
