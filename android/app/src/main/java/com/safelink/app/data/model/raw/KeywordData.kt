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
    @SerializedName("match_type") val matchType: String,   // "keyword" | "regex-simple"
    val keyword: String? = null,
    val pattern: String? = null,
    val weight: Int,
    @SerializedName("repeat_decay") val repeatDecay: Boolean = true,
    @SerializedName("structural_only") val structuralOnly: Boolean = false,
    val description: String,
    val source: String
)

data class ComboBonusRule(
    val id: String,
    val type: String,           // "general" | "specific"
    val category: String,       // "any" 또는 특정 category명
    val condition: String,
    @SerializedName("min_distinct_subcategories") val minDistinctSubcategories: Int? = null,
    @SerializedName("window_turns") val windowTurns: Int? = null,
    val bonus: Int,
    @SerializedName("related_keyword_ids") val relatedKeywordIds: List<String>? = null,
    @SerializedName("related_subcategory_ids") val relatedSubcategoryIds: List<String>? = null
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
