package com.safelink.app.data.model.raw

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * assets/institutions.json 원본 구조 1:1 매핑 (Gson 파싱용).
 * UI에 노출되는 RecommendedInstitutionUi와는 별개 — DetectionRepository에서 변환해서 사용.
 *
 * subcategory_to_risk_type / keyword_risk_type_additions는 JSON에 "note" 설명 문자열이
 * 실제 매핑 항목들과 같은 객체 레벨에 섞여 있어서, 우선 JsonElement로 받은 뒤
 * InstitutionRawData.subcategoryMappings() / keywordAdditions()에서 note를 걸러내고
 * 객체인 항목만 골라 변환한다.
 */
data class InstitutionData(
    val institutions: List<InstitutionEntry>,
    @SerializedName("risk_type_priority") val riskTypePriority: Map<String, List<InstitutionPriorityEntry>>,
    @SerializedName("subcategory_to_risk_type") private val subcategoryToRiskTypeRaw: Map<String, JsonElement>,
    @SerializedName("keyword_risk_type_additions") private val keywordRiskTypeAdditionsRaw: Map<String, JsonElement>
) {
    fun subcategoryMappings(gson: com.google.gson.Gson): Map<String, SubcategoryRiskTypeMapping> =
        subcategoryToRiskTypeRaw
            .filterValues { it.isJsonObject }
            .mapValues { (_, element) -> gson.fromJson(element, SubcategoryRiskTypeMapping::class.java) }

    fun keywordAdditions(gson: com.google.gson.Gson): Map<String, KeywordRiskTypeAddition> =
        keywordRiskTypeAdditionsRaw
            .filterValues { it.isJsonObject }
            .mapValues { (_, element) -> gson.fromJson(element, KeywordRiskTypeAddition::class.java) }
}

data class InstitutionEntry(
    val id: String,
    val name: String,
    val tier: String,
    val group: String,
    val role: String,
    val contact: String
)

data class InstitutionPriorityEntry(
    @SerializedName("institution_id") val institutionId: String,
    val rank: Int,
    val reason: String
)

data class SubcategoryRiskTypeMapping(
    @SerializedName("risk_types") val riskTypes: List<String>,
    val weight: Int,
    @SerializedName("standalone_recommend") val standaloneRecommend: Boolean
)

data class KeywordRiskTypeAddition(
    @SerializedName("add_risk_types") val addRiskTypes: List<String>,
    val reason: String
)
