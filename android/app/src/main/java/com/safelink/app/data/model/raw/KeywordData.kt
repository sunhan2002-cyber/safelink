package com.safelink.app.data.model.raw

import com.google.gson.annotations.SerializedName

/**
 * assets/keyword.json 원본 구조 1:1 매핑 (Gson 파싱용).
 * UI에 노출되는 MatchedKeyword와는 별개 — DetectionRepository에서 변환해서 사용.
 */
data class KeywordData(
    val keywords: List<KeywordEntry>,
    @SerializedName("combo_bonus_rules") val comboBonusRules: List<ComboBonusRule>,
    @SerializedName("repeat_decay_policy") val repeatDecayPolicy: RepeatDecayPolicy,
    @SerializedName("risk_level_bands") val riskLevelBands: List<RiskLevelBand>
)

data class KeywordEntry(
    val id: String,
    val category: String,
    @SerializedName("subcategory_id") val subcategoryId: String,
    val subcategory: String,
    @SerializedName("match_type") val matchType: String,   // "keyword" | "regex-simple" | "regex-complex"
    val keyword: String? = null,
    val pattern: String? = null,
    val weight: Int,
    @SerializedName("repeat_decay") val repeatDecay: Boolean = true,
    @SerializedName("structural_only") val structuralOnly: Boolean = false,
    val description: String,
    val source: String,
    // match_type이 regex-complex일 때만 사용 - "숫자 값에 따라 위험 신호 여부가 달라지는
    // 문장 규칙"(예: "100만원만" 같은 소액한정 요구, 큰 금액이면 오히려 무해) 대응.
    // pattern의 캡처그룹(기본 1번째)에서 숫자를 뽑아 numeric_min~numeric_max 범위 안일 때만
    // 매칭으로 인정한다 (둘 다 null이면 범위 체크 없이 regex-simple과 동일하게 동작).
    @SerializedName("numeric_capture_group") val numericCaptureGroup: Int = 1,
    @SerializedName("numeric_min") val numericMin: Int? = null,
    @SerializedName("numeric_max") val numericMax: Int? = null
)

data class ComboBonusRule(
    val id: String,
    val type: String,           // "general" | "specific" | "repeat_pattern" | "long_session_pattern"
    val category: String,       // "any" 또는 특정 category명
    val condition: String,
    @SerializedName("min_distinct_subcategories") val minDistinctSubcategories: Int? = null,
    @SerializedName("window_turns") val windowTurns: Int? = null,
    val bonus: Int,
    @SerializedName("related_keyword_ids") val relatedKeywordIds: List<String>? = null,
    @SerializedName("related_subcategory_ids") val relatedSubcategoryIds: List<String>? = null,
    // repeat_pattern/long_session_pattern 전용 (any-of 의미 - related_subcategory_ids의
    // all-of 의미와 다름): subcategory_ids 중 하나라도 매칭되면 대상으로 집계한다.
    @SerializedName("subcategory_ids") val subcategoryIds: List<String>? = null,
    @SerializedName("min_match_count") val minMatchCount: Int? = null,
    @SerializedName("min_turns") val minTurns: Int? = null
)

data class RepeatDecayPolicy(
    @SerializedName("first_occurrence_multiplier") val firstOccurrenceMultiplier: Double,
    @SerializedName("subsequent_occurrence_multiplier") val subsequentOccurrenceMultiplier: Double
)

data class RiskLevelBand(
    val level: String,   // "낮음" | "중간" | "높음"
    val min: Int,
    val max: Int
)
