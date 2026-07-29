package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.MatchedKeyword
import com.safelink.app.data.model.RecommendedInstitutionUi
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.model.raw.ComboBonusRule
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.InstitutionPriorityEntry
import com.safelink.app.data.model.raw.KeywordData
import com.safelink.app.data.model.raw.KeywordEntry
import kotlin.math.min

/**
 * 온디바이스 위험 감지 로직 (SafeLink 아키텍처 원칙 1: 위험도 계산은 항상 온디바이스).
 * Android(Context/assets)에 의존하지 않는 순수 Kotlin 클래스 — [KeywordData]/[InstitutionData]는
 * 이미 파싱된 상태로 주입받는다. Android 쪽 로딩은 [DetectionRepository]가 담당.
 *
 * 데이터 근거:
 * - 키워드 매칭/점수: keyword.json (keywords, combo_bonus_rules, repeat_decay_policy)
 * - 추천 기관: institutions.json (subcategory_to_risk_type, risk_type_priority,
 *   keyword_risk_type_additions)
 * - resolve 흐름 근거: data/API 입출력 초안 v1 3-1장
 *
 * 진입점은 [analyze] 하나.
 */
class DetectionEngine(
    private val keywordData: KeywordData,
    institutionData: InstitutionData,
    gson: Gson = Gson()
) {
    private val subcategoryMappings = institutionData.subcategoryMappings(gson)
    private val keywordAdditions = institutionData.keywordAdditions(gson)
    private val institutionsById = institutionData.institutions.associateBy { it.id }
    private val riskTypePriority: Map<String, List<InstitutionPriorityEntry>> = institutionData.riskTypePriority

    /** 원문 텍스트 1건을 분석해서 [DetectionResult]로 변환. 화면/ViewModel에서 사용하는 기본 진입점. */
    fun analyze(originalText: String): DetectionResult = analyze(listOf(originalText))

    /** 여러 턴(대화)을 이어서 분석. 콤보 판정 등 세션 단위 로직 검증에 사용. */
    fun analyze(turns: List<String>): DetectionResult {
        val originalText = turns.joinToString(" ")
        val turnOffsets = turnOffsets(turns)

        val rawMatches = turns.flatMapIndexed { turnIndex, turnText ->
            matchKeywordsInTurn(turnText, turnIndex, turnOffsets[turnIndex])
        }

        val (baseScore, matchedKeywords) = scoreMatches(rawMatches)
        val comboIds = evaluateComboRules(rawMatches)
        val comboBonus = comboIds.sumOf { id -> keywordData.comboBonusRules.first { it.id == id }.bonus }
        val totalScore = min(baseScore + comboBonus, 100.0)
        val score = totalScore.toInt()

        val riskLevel = RiskLevel.fromScore(score)
        val category = rawMatches.groupBy { it.entry.category }
            .maxByOrNull { (_, group) -> group.sumOf { it.entry.weight } }
            ?.key ?: ""

        val recommendedInstitutions = resolveInstitutions(rawMatches, comboIds, score)

        return DetectionResult(
            riskLevel = riskLevel,
            score = score,
            category = category,
            originalText = originalText,
            matchedKeywords = matchedKeywords,
            recommendedInstitutions = recommendedInstitutions,
            appliedComboIds = comboIds
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // 1. 키워드 매칭 (요청 2번: 어떤 키워드를 어떤 규칙으로 잡을지)
    // ─────────────────────────────────────────────────────────────────

    private data class RawMatch(
        val entry: KeywordEntry,
        val turnIndex: Int,
        val startInFull: Int,
        val endInFull: Int,
        val matchedText: String
    )

    private fun turnOffsets(turns: List<String>): List<Int> {
        val offsets = mutableListOf<Int>()
        var pos = 0
        turns.forEachIndexed { i, t ->
            offsets.add(pos)
            pos += t.length
            if (i < turns.size - 1) pos += 1 // joinToString(" ") 구분자
        }
        return offsets
    }

    private fun matchKeywordsInTurn(turnText: String, turnIndex: Int, turnOffset: Int): List<RawMatch> {
        val matches = mutableListOf<RawMatch>()
        for (entry in keywordData.keywords) {
            when (entry.matchType) {
                "keyword" -> {
                    val needle = entry.keyword ?: continue
                    var searchFrom = 0
                    while (true) {
                        val idx = turnText.indexOf(needle, searchFrom)
                        if (idx < 0) break
                        matches += RawMatch(
                            entry = entry,
                            turnIndex = turnIndex,
                            startInFull = turnOffset + idx,
                            endInFull = turnOffset + idx + needle.length,
                            matchedText = needle
                        )
                        searchFrom = idx + needle.length
                    }
                }
                "regex-simple" -> {
                    val pattern = entry.pattern ?: continue
                    Regex(pattern).findAll(turnText).forEach { m ->
                        matches += RawMatch(
                            entry = entry,
                            turnIndex = turnIndex,
                            startInFull = turnOffset + m.range.first,
                            endInFull = turnOffset + m.range.last + 1,
                            matchedText = m.value
                        )
                    }
                }
            }
        }
        return matches
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. 점수 계산 (요청 3번: repeat_decay + combo_bonus_rules)
    // ─────────────────────────────────────────────────────────────────

    private fun scoreMatches(rawMatches: List<RawMatch>): Pair<Double, List<MatchedKeyword>> {
        val policy = keywordData.repeatDecayPolicy
        var total = 0.0
        val matchedKeywords = mutableListOf<MatchedKeyword>()

        val bySubcategory = rawMatches.groupBy { it.entry.subcategoryId }
        for ((_, group) in bySubcategory) {
            val decaying = group.filter { it.entry.repeatDecay }.sortedBy { it.startInFull }
            val nonDecaying = group.filter { !it.entry.repeatDecay }

            decaying.forEachIndexed { occurrenceIndex, m ->
                val multiplier = if (occurrenceIndex == 0) {
                    policy.firstOccurrenceMultiplier
                } else {
                    policy.subsequentOccurrenceMultiplier
                }
                val contribution = if (m.entry.structuralOnly) 0.0 else m.entry.weight * multiplier
                total += contribution
                matchedKeywords += m.toMatchedKeyword()
            }
            nonDecaying.forEach { m ->
                val contribution = if (m.entry.structuralOnly) 0.0 else m.entry.weight.toDouble()
                total += contribution
                matchedKeywords += m.toMatchedKeyword()
            }
        }
        return total to matchedKeywords.sortedBy { it.startIndex }
    }

    private fun RawMatch.toMatchedKeyword() = MatchedKeyword(
        keywordId = entry.id,
        subcategoryId = entry.subcategoryId,
        subcategoryName = entry.subcategory,
        matchedText = matchedText,
        startIndex = startInFull,
        endIndex = endInFull,
        weight = entry.weight,
        description = entry.description
    )

    // ─────────────────────────────────────────────────────────────────
    // 콤보 보너스 판정
    //
    // COMBO-GENERAL-3CAT/4CAT은 min_distinct_subcategories 조건을 그대로 계산해서 판정하지만,
    // 나머지 4개 specific 콤보는 조건문(트리거 대상 id 조합)이 서로 달라 keyword.json의
    // condition 설명을 그대로 코드화했다 (일반화된 엔진이 아니라 v1 초안 - 남은 작업 참고).
    // ─────────────────────────────────────────────────────────────────

    private fun evaluateComboRules(rawMatches: List<RawMatch>): List<String> {
        if (rawMatches.isEmpty()) return emptyList()
        val matchedIds = rawMatches.map { it.entry.id }.toSet()
        val subcategoriesByCategory = rawMatches.groupBy { it.entry.category }
            .mapValues { (_, list) -> list.map { it.entry.subcategoryId }.toSet() }

        val triggered = mutableListOf<String>()

        val maxDistinct = subcategoriesByCategory.values.maxOfOrNull { it.size } ?: 0
        when {
            maxDistinct >= 4 -> triggered += "COMBO-GENERAL-4CAT"
            maxDistinct >= 3 -> triggered += "COMBO-GENERAL-3CAT"
        }

        if ("VP-1-3-003" in matchedIds && ("VP-1-3-001" in matchedIds || "VP-1-3-002" in matchedIds)) {
            triggered += "COMBO-VP-PHONE-VERIFY"
        }
        if ("VP-1-6-004" in matchedIds &&
            listOf("VP-1-6-001", "VP-1-6-002", "VP-1-6-003").any { it in matchedIds }
        ) {
            triggered += "COMBO-VP-SMISHING"
        }

        val subcategoryIds = rawMatches.map { it.entry.subcategoryId }.toSet()
        if ("2-3" in subcategoryIds && "2-4" in subcategoryIds) triggered += "COMBO-RS-SECRET-MONEY"
        if ("3-3" in subcategoryIds && "3-4" in subcategoryIds) triggered += "COMBO-GL-ISOLATION-GUILT"

        return triggered
    }

    /** 이 콤보 판정에 실제로 관여한 subcategory_id 집합 (추천기관 게이팅에서 사용). */
    private fun subcategoriesCoveredByCombos(comboIds: List<String>, rawMatches: List<RawMatch>): Set<String> {
        if (comboIds.isEmpty()) return emptySet()
        val covered = mutableSetOf<String>()
        val rulesById = keywordData.comboBonusRules.associateBy(ComboBonusRule::id)

        for (comboId in comboIds) {
            val rule = rulesById[comboId] ?: continue
            when {
                rule.type == "general" -> {
                    // 이 콤보를 트리거한 카테고리 안에서 매칭된 모든 subcategory가 콤보의 일부
                    val category = rawMatches.groupBy { it.entry.category }
                        .maxByOrNull { (_, list) -> list.map { it.entry.subcategoryId }.toSet().size }
                        ?.key
                    covered += rawMatches.filter { it.entry.category == category }.map { it.entry.subcategoryId }
                }
                rule.relatedSubcategoryIds != null -> covered += rule.relatedSubcategoryIds
                rule.relatedKeywordIds != null -> covered += rawMatches
                    .filter { it.entry.id in rule.relatedKeywordIds }
                    .map { it.entry.subcategoryId }
            }
        }
        return covered
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. 추천 기관 resolve (요청 5번) — API 입출력 초안 v1 3-1장 흐름
    //    subcategory_id -> (standalone_recommend 게이팅) -> risk_types -> risk_type_priority
    //    -> 병합/dedup -> rank 1부터 재부여
    // ─────────────────────────────────────────────────────────────────

    private fun resolveInstitutions(
        rawMatches: List<RawMatch>,
        comboIds: List<String>,
        totalScore: Int
    ): List<RecommendedInstitutionUi> {
        val comboCoveredSubcategories = subcategoriesCoveredByCombos(comboIds, rawMatches)
        val matchedSubcategoryIds = rawMatches.map { it.entry.subcategoryId }.toSet()

        // 1) 게이팅 통과한 subcategory만 남긴다
        val gatedSubcategoryIds = matchedSubcategoryIds.filter { subId ->
            val mapping = subcategoryMappings[subId] ?: return@filter false
            mapping.standaloneRecommend || subId in comboCoveredSubcategories || totalScore >= 31
        }
        if (gatedSubcategoryIds.isEmpty()) return emptyList()

        // 2) subcategory -> risk_types (keyword_risk_type_additions 반영)
        val riskTypesBySubcategory: Map<String, Set<String>> = gatedSubcategoryIds.associateWith { subId ->
            val base = subcategoryMappings[subId]?.riskTypes.orEmpty()
            val additions = rawMatches
                .filter { it.entry.subcategoryId == subId }
                .flatMap { keywordAdditions[it.entry.id]?.addRiskTypes.orEmpty() }
            (base + additions).toSet()
        }

        // 3) risk_type 노출 순서: 이 risk_type을 만든 subcategory 중 가장 weight가 높은 순
        val riskTypeMaxWeight = mutableMapOf<String, Int>()
        for (subId in gatedSubcategoryIds) {
            val weight = subcategoryMappings[subId]?.weight ?: 0
            riskTypesBySubcategory[subId]?.forEach { riskType ->
                riskTypeMaxWeight[riskType] = maxOf(riskTypeMaxWeight[riskType] ?: 0, weight)
            }
        }
        val orderedRiskTypes = riskTypeMaxWeight.entries.sortedByDescending { it.value }.map { it.key }

        // 4) risk_type_priority 순서대로 합치면서 institution_id 기준 dedup (첫 등장만 유지)
        val merged = LinkedHashMap<String, RecommendedInstitutionUi>()
        for (riskType in orderedRiskTypes) {
            val priorityList = riskTypePriority[riskType].orEmpty().sortedBy { it.rank }
            for (p in priorityList) {
                if (merged.containsKey(p.institutionId)) continue
                val institution = institutionsById[p.institutionId] ?: continue
                merged[p.institutionId] = RecommendedInstitutionUi(
                    institutionId = institution.id,
                    name = institution.name,
                    contact = institution.contact,
                    rank = 0, // 아래서 배열 순서 기준으로 재부여
                    reason = p.reason,
                    matchedRiskType = riskType,
                    group = institution.group
                )
            }
        }

        // 5) 최종 배열 인덱스 기준으로 rank 1부터 재부여
        return merged.values.mapIndexed { index, ui -> ui.copy(rank = index + 1) }
    }
}
