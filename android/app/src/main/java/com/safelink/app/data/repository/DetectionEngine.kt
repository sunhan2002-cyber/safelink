package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.AnalysisEvidence
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.MatchedKeyword
import com.safelink.app.data.model.RecommendedInstitutionUi
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.model.raw.ComboBonusRule
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.InstitutionPriorityEntry
import com.safelink.app.data.model.raw.KeywordData
import com.safelink.app.data.model.raw.KeywordEntry
import com.safelink.app.data.remote.dto.AnalyzeResponseDto
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

    /**
     * 서버로 보내기 전 마스킹. keyword.json의 전화번호(VP-1-3-003)/URL(VP-1-6-004) regex를
     * 새로 만들지 않고 그대로 재사용해서 [전화번호]/[링크]로 치환한다 (data/API 입출력
     * 초안 v1 "마스킹 규칙" 그대로). 이 두 id가 keyword.json에서 사라지면 마스킹도 같이
     * 깨지므로, structural_only 항목을 지울 때는 이 함수도 같이 확인해야 함.
     */
    fun maskSensitiveInfo(text: String): String {
        val phonePattern = keywordData.keywords.first { it.id == "VP-1-3-003" }.pattern!!
        val urlPattern = keywordData.keywords.first { it.id == "VP-1-6-004" }.pattern!!
        return text
            .replace(Regex(phonePattern), "[전화번호]")
            .replace(Regex(urlPattern), "[링크]")
    }

    /** 여러 턴(대화)을 이어서 분석. 콤보 판정 등 세션 단위 로직 검증에 사용. */
    fun analyze(turns: List<String>): DetectionResult {
        val originalText = turns.joinToString(" ")
        val turnOffsets = turnOffsets(turns)

        val rawMatches = turns.flatMapIndexed { turnIndex, turnText ->
            matchKeywordsInTurn(turnText, turnIndex, turnOffsets[turnIndex])
        }

        val suppressed = findSuppressedByOverlap(rawMatches)
        val (baseScore, matchedKeywords) = scoreMatches(rawMatches, suppressed)
        val comboIds = evaluateComboRules(rawMatches, suppressed, turnCount = turns.size, originalText = originalText)
        val comboBonus = comboIds.sumOf { id -> keywordData.comboBonusRules.first { it.id == id }.bonus }
        val totalScore = min(baseScore + comboBonus, 100.0)
        val score = totalScore.toInt()

        val riskLevel = RiskLevel.fromScore(score)
        val category = rawMatches.groupBy { it.entry.category }
            .maxByOrNull { (_, group) -> group.sumOf { it.entry.weight } }
            ?.key ?: ""

        val recommendedInstitutions = resolveInstitutions(rawMatches, comboIds, score)
        val evidences = buildEvidences(rawMatches, suppressed, comboIds)

        return DetectionResult(
            riskLevel = riskLevel,
            score = score,
            category = category,
            originalText = originalText,
            matchedKeywords = matchedKeywords,
            recommendedInstitutions = recommendedInstitutions,
            appliedComboIds = comboIds,
            evidences = evidences
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
                "regex-complex" -> {
                    // 문장 규칙(5주차 팀 목표): 정규식만으로는 부족하고 캡처된 숫자값까지
                    // 조건에 넣어야 하는 패턴 - 예: "100만원만요"는 소액한정 요구(SMALL_ASK)
                    // 신호지만 "1000만원만요"는 오히려 큰 금액이라 같은 신호로 볼 수 없음.
                    val pattern = entry.pattern ?: continue
                    Regex(pattern).findAll(turnText).forEach { m ->
                        val numberGroup = m.groups[entry.numericCaptureGroup]?.value?.toIntOrNull()
                        val inRange = numberGroup != null &&
                            (entry.numericMin == null || numberGroup >= entry.numericMin) &&
                            (entry.numericMax == null || numberGroup <= entry.numericMax)
                        if (inRange) {
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
        }
        return matches
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. 점수 계산 (요청 3번: repeat_decay + combo_bonus_rules)
    // ─────────────────────────────────────────────────────────────────

    private fun scoreMatches(rawMatches: List<RawMatch>, suppressed: Set<RawMatch>): Pair<Double, List<MatchedKeyword>> {
        val policy = keywordData.repeatDecayPolicy
        var total = 0.0
        val matchedKeywords = mutableListOf<MatchedKeyword>()

        val bySubcategory = rawMatches.groupBy { it.entry.subcategoryId }
        for ((_, group) in bySubcategory) {
            val scorable = group.filterNot { it in suppressed }
            val decaying = scorable.filter { it.entry.repeatDecay }.sortedBy { it.startInFull }
            val nonDecaying = scorable.filter { !it.entry.repeatDecay }

            decaying.forEachIndexed { occurrenceIndex, m ->
                val multiplier = if (occurrenceIndex == 0) {
                    policy.firstOccurrenceMultiplier
                } else {
                    policy.subsequentOccurrenceMultiplier
                }
                val contribution = if (m.entry.structuralOnly) 0.0 else m.entry.weight * multiplier
                total += contribution
            }
            nonDecaying.forEach { m ->
                val contribution = if (m.entry.structuralOnly) 0.0 else m.entry.weight.toDouble()
                total += contribution
            }
            // matchedKeywords는 억제 여부와 무관하게 전부 기록 - 화면에는 "이 표현도 감지됨"을
            // 계속 보여주고, 점수 중복 계산만 막는다 (표시와 점수 반영 분리).
            group.forEach { matchedKeywords += it.toMatchedKeyword() }
        }
        return total to matchedKeywords.sortedBy { it.startIndex }
    }

    /**
     * 서로 다른 키워드 id의 매칭 구간이 겹칠 때(한쪽이 다른쪽에 완전히 포함되거나 구간이
     * 동일할 때), 더 짧은(덜 구체적인) 쪽을 점수 계산에서 제외한다. 예: "오늘 안에"(VP-1-4-003)가
     * "오늘 안에 처리 안되면"(RS-2-5-001) 안에 완전히 포함되는 경우 - 같은 표현을 두 신호로
     * 중복 계산하지 않기 위함 (신기훈 4주차 04번 문서 "부분 문자열 충돌" 분석 결과 반영).
     * 구간 길이가 같으면 keyword.json에 먼저 등록된 쪽을 유지한다(결정론적 tie-break).
     */
    private fun findSuppressedByOverlap(rawMatches: List<RawMatch>): Set<RawMatch> {
        val suppressed = mutableSetOf<RawMatch>()
        for (i in rawMatches.indices) {
            val a = rawMatches[i]
            for (j in rawMatches.indices) {
                if (i == j) continue
                val b = rawMatches[j]
                if (a.entry.id == b.entry.id) continue
                val aInB = a.startInFull >= b.startInFull && a.endInFull <= b.endInFull
                if (!aInB) continue
                val aLength = a.endInFull - a.startInFull
                val bLength = b.endInFull - b.startInFull
                val aStrictlyShorter = aLength < bLength
                val aLosesTie = aLength == bLength && j < i
                if (aStrictlyShorter || aLosesTie) suppressed += a
            }
        }
        return suppressed
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
    // COMBO-GENERAL-3CAT/4CAT은 min_distinct_subcategories 조건을 그대로 계산해서 판정한다.
    // related_subcategory_ids가 있는 specific 콤보(세션 전체에서 지정된 subcategory가 전부
    // 매칭되면 발동)는 아래에서 keyword.json을 순회하며 일반화해서 처리한다.
    // COMBO-VP-PHONE-VERIFY/COMBO-VP-SMISHING만 "특정 id 조합 + OR 조건"이라 예외적으로
    // 하드코딩되어 있다 (related_keyword_ids 기반, 전부 매칭이 아니라 일부만 있어도 되는 경우).
    //
    // repeat_pattern/long_session_pattern: shouldEscalateToAI()가 "AI를 부를지"에만 쓰던
    // 반복/장기세션 신호(가스라이팅 반복, 장기서사형 로맨스스캠)를 온디바이스 점수 계산
    // 본체에도 반영하기 위해 4주차 수정 2 리뷰 대응으로 추가함 - subcategory_ids는 any-of
    // (하나라도 매칭되면 집계 대상)라서 related_subcategory_ids(all-of)와 별도 처리한다.
    // AI 호출 여부 판단(shouldEscalateToAI)과는 독립적으로 동작 - 이 콤보가 발동해도 AI 호출
    // 여부와 무관하고, AI가 호출되든 안 되든 이 점수는 항상 온디바이스에서만 계산된다.
    //
    // numeric_ratio_pattern: NUMERIC_REASONING 갭(예: "3주만에 100이 137 됐죠?" - 37% 수익률을
    // 이해해야 위험 신호라는 걸 아는 경우) 스트레치 대응. 원문에서 숫자 두 개를 뽑아 증가율을
    // 계산하는 방식이라, 매칭된 키워드가 하나도 없어도(rawMatches가 비어도) 평가해야 해서
    // 이 함수는 rawMatches 존재 여부와 무관하게 항상 끝까지 실행한다.
    // ─────────────────────────────────────────────────────────────────

    private fun evaluateComboRules(
        rawMatches: List<RawMatch>,
        suppressed: Set<RawMatch>,
        turnCount: Int,
        originalText: String
    ): List<String> {
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

        // related_subcategory_ids 기반 "전부 매칭 시 발동" 규칙 - 세션 전체 기준으로 자동 판정
        // (RS-SECRET-MONEY, GL-ISOLATION-GUILT 및 4주차에 추가된 5개 콤보 모두 이 형태라 하드코딩 대신 일반화)
        val subcategoryIds = rawMatches.map { it.entry.subcategoryId }.toSet()
        keywordData.comboBonusRules
            .filter { it.type == "specific" && it.relatedSubcategoryIds != null }
            .forEach { rule ->
                if (rule.relatedSubcategoryIds!!.all { it in subcategoryIds }) {
                    triggered += rule.id
                }
            }

        // repeat_pattern: subcategory_ids(any-of) 매칭이 세션 전체에서 min_match_count 이상.
        // suppressed(겹침 억제 - 04번 문서 "부분 문자열 충돌") 제외한 매칭만 센다 - 같은 표현이
        // 두 keyword id(예: 원문 그대로 + 문구변형 대응 regex)로 중복 매칭된 걸 "반복 2회"로
        // 잘못 세지 않기 위함. 점수 계산(scoreMatches)과 동일한 억제 기준을 공유한다.
        val nonSuppressed = rawMatches.filterNot { it in suppressed }
        keywordData.comboBonusRules
            .filter { it.type == "repeat_pattern" && it.subcategoryIds != null && it.minMatchCount != null }
            .forEach { rule ->
                val matchCount = nonSuppressed.count { it.entry.subcategoryId in rule.subcategoryIds!! }
                if (matchCount >= rule.minMatchCount!!) triggered += rule.id
            }

        // long_session_pattern: subcategory_ids(any-of) 매칭 + 세션 턴 수가 min_turns 이상
        keywordData.comboBonusRules
            .filter { it.type == "long_session_pattern" && it.subcategoryIds != null && it.minTurns != null }
            .forEach { rule ->
                val hasMatch = nonSuppressed.any { it.entry.subcategoryId in rule.subcategoryIds!! }
                if (hasMatch && turnCount >= rule.minTurns!!) triggered += rule.id
            }

        // numeric_ratio_pattern: pattern의 캡처그룹 1·2번을 "이전 값"·"이후 값"으로 보고
        // 증가율(%)을 계산 - min_growth_rate_percent 이상이면 발동. 키워드 매칭과 무관하게
        // 원문 전체(originalText)에 대해 정규식을 돌린다.
        keywordData.comboBonusRules
            .filter { it.type == "numeric_ratio_pattern" && it.pattern != null && it.minGrowthRatePercent != null }
            .forEach { rule ->
                val match = Regex(rule.pattern!!).find(originalText)
                val before = match?.groups?.get(1)?.value?.toDoubleOrNull()
                val after = match?.groups?.get(2)?.value?.toDoubleOrNull()
                if (before != null && after != null && before > 0) {
                    val growthRatePercent = (after - before) / before * 100.0
                    if (growthRatePercent >= rule.minGrowthRatePercent!!) triggered += rule.id
                }
            }

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
                rule.subcategoryIds != null -> covered += rule.subcategoryIds
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

    // ─────────────────────────────────────────────────────────────────
    // 4. 2차 AI API 보조 분석 호출 판단 (요청: "규칙 엔진으로 안 잡히는 경우 어떤 기준에서
    //    AI API로 넘길지"). 신기훈 4주차 06번 문서 "AI API 진입 조건 확정본" 그대로 코드화.
    //    실제 API 호출은 이 함수 밖(김재겸 쪽, 백엔드 미구현)에서 처리 — 여기는 "호출해야
    //    하는가"만 결정한다. data/API 입출력 초안 v1의 회색지대 조건 + 06번 문서 신규 조건 4개.
    // ─────────────────────────────────────────────────────────────────

    companion object {
        /** 4주차에 신설된 중분류 - 실사용 검증 전까지 한시적으로 회색지대 무관 호출. */
        private val NEW_SUBCATEGORIES_2026_07 = setOf("2-6", "2-7", "2-8", "2-9", "2-10", "3-7", "3-8", "3-9")

        private val GRAY_ZONE_SCORE_RANGES = listOf(20..40, 55..70)
    }

    /**
     * 2차 AI API 보조 분석을 호출해야 하는지 판단한다. 위험도 계산 자체는 항상 온디바이스로
     * 끝나며(SafeLink 아키텍처 원칙), 이 함수는 "문맥 보정치를 받아올 필요가 있는가"만 결정한다.
     *
     * 5주차 정리: 가스라이팅 반복 2회+/장기세션 15턴+ 트리거는 여기서 제거했다 — 4주차 수정2
     * 리뷰 대응으로 이미 `COMBO-GL-REPEAT-PATTERN`/`COMBO-RS-LONG-SESSION-PATTERN`(온디바이스
     * 점수 콤보)에 반영돼 있어서, 같은 패턴을 "AI를 또 부르는 이유"로 중복 유지할 필요가
     * 없어졌다. 이제 이 함수는 정말 "애매한 경우"(회색지대 점수·수동신고·신뢰도 낮은 신규
     * subcategory)만 남는다.
     *
     * @param result [analyze]가 반환한 온디바이스 분석 결과
     * @param manualReportFlag 사용자가 직접 "위험한 것 같다"고 표시했는지
     * @param newSubcategoryRolloutActive 4주차 신설 중분류에 대한 한시적 게이팅 스위치 -
     *   실사용 데이터가 충분히 쌓이면 false로 전환해서 이 조건을 끄면 됨
     */
    fun shouldEscalateToAI(
        result: DetectionResult,
        manualReportFlag: Boolean = false,
        newSubcategoryRolloutActive: Boolean = true
    ): Boolean {
        if (manualReportFlag) return true

        if (GRAY_ZONE_SCORE_RANGES.any { result.score in it }) return true

        if (newSubcategoryRolloutActive) {
            val matchedSubcategoryIds = result.matchedKeywords.map { it.subcategoryId }
            if (matchedSubcategoryIds.any { it in NEW_SUBCATEGORIES_2026_07 }) return true
        }

        return false
    }

    // ─────────────────────────────────────────────────────────────────
    // 5. AI 응답 병합 (신기훈 4주차 07번 문서 "AI 응답 반영 범위" 결정 반영)
    //
    // 반영하는 것: context_score_adjustment(점수), context_analysis_summary/
    // context_detected_pattern(문구 2개), recommended_institutions(추천기관 - 온디바이스
    // 목록과 병합).
    // 반영 안 하는 것: recommended_level_override — 서버는 3단계(낮음/중간/높음), 클라이언트
    // RiskLevel은 4단계라 자동 매핑이 모호함(예: "높음"이 WARNING인지 CRITICAL인지 점수 없이는
    // 알 수 없음). "서버가 최종 위험도를 결정하지 않는다"는 원칙 그대로 점수 보정치만 반영하고
    // riskLevel은 항상 adjustedScore로부터 다시 계산한다.
    // ─────────────────────────────────────────────────────────────────

    /**
     * 온디바이스 결과에 AI 보조 분석 응답을 병합한다. [DetectionRepository.escalateToAI]가
     * 네트워크 호출 성공 시 이 함수를 호출한다 — 병합 규칙 자체는 여기 한 곳에만 있음.
     */
    fun mergeAiResponse(original: DetectionResult, response: AnalyzeResponseDto): DetectionResult {
        val adjustedScore = (original.score + response.contextScoreAdjustment).toInt().coerceIn(0, 100)

        // AI 근거는 온디바이스 근거(evidences) 뒤에 이어붙인다 - 화면에서 "키워드/문장 규칙/
        // 상황 규칙 근거를 먼저 보여주고 AI 보조분석 근거는 마지막에" 순서로 자연스럽게 나열 가능.
        val aiEvidence = AnalysisEvidence(
            source = AnalysisEvidence.SOURCE_AI,
            label = response.contextDetectedPattern ?: "AI 보조분석 결과",
            detail = response.contextAnalysisSummary
        )

        return original.copy(
            score = adjustedScore,
            riskLevel = RiskLevel.fromScore(adjustedScore),
            recommendedInstitutions = mergeInstitutions(original.recommendedInstitutions, response),
            aiSummary = response.contextAnalysisSummary,
            aiDetectedPattern = response.contextDetectedPattern,
            evidences = original.evidences + aiEvidence
        )
    }

    /**
     * 서버가 추천한 institution_id를 institutions.json에서 조회해 온디바이스 추천 목록과
     * 합친다. 서버 응답에는 institution_id/rank/reason/matched_risk_type만 있고
     * name/contact/group이 없어서(그건 온디바이스 institutions.json에만 있음) 여기서 조회해서
     * 채운다. 이미 온디바이스 목록에 있는 기관(institution_id 동일)은 온디바이스 쪽을
     * 우선하고, 최종 배열 인덱스 기준으로 rank를 1부터 재부여한다 (resolveInstitutions와
     * 동일한 dedup/rerank 원칙).
     */
    private fun mergeInstitutions(
        onDevice: List<RecommendedInstitutionUi>,
        response: AnalyzeResponseDto
    ): List<RecommendedInstitutionUi> {
        if (response.recommendedInstitutions.isEmpty()) return onDevice

        val fromServer = response.recommendedInstitutions.mapNotNull { dto ->
            institutionsById[dto.institutionId]?.let { institution ->
                RecommendedInstitutionUi(
                    institutionId = institution.id,
                    name = institution.name,
                    contact = institution.contact,
                    rank = 0, // 아래서 재부여
                    reason = dto.reason,
                    matchedRiskType = dto.matchedRiskType,
                    group = institution.group
                )
            }
        }

        return (onDevice + fromServer)
            .distinctBy { it.institutionId } // 온디바이스가 먼저 오므로 겹치면 온디바이스 쪽 유지
            .mapIndexed { index, ui -> ui.copy(rank = index + 1) }
    }

    // ─────────────────────────────────────────────────────────────────
    // 6. 분석 근거 정리 (5주차 신설 - 결과 화면에 "왜 이 점수인지" 넘길 데이터, AnalysisEvidence 참고)
    //
    // rawMatches -> 키워드/문장 규칙 근거 (match_type 기준), comboIds -> 키워드조합/문장규칙/
    // 상황규칙 근거 (combo type 기준). AI 근거는 여기서 안 만듦 - analyze() 시점엔 AI 응답이
    // 아직 없으므로, mergeAiResponse()가 응답을 병합할 때 AI 근거를 별도로 추가한다.
    // ─────────────────────────────────────────────────────────────────

    private fun buildEvidences(
        rawMatches: List<RawMatch>,
        suppressed: Set<RawMatch>,
        comboIds: List<String>
    ): List<AnalysisEvidence> {
        val keywordEvidences = rawMatches
            .filterNot { it in suppressed }
            .distinctBy { it.entry.id }
            .map { m ->
                val source = if (m.entry.matchType == "regex-complex") {
                    AnalysisEvidence.SOURCE_SENTENCE_RULE
                } else {
                    AnalysisEvidence.SOURCE_KEYWORD
                }
                AnalysisEvidence(source = source, label = m.entry.description, detail = m.matchedText)
            }

        val comboEvidences = comboIds.mapNotNull { id ->
            val rule = keywordData.comboBonusRules.firstOrNull { it.id == id } ?: return@mapNotNull null
            val source = when (rule.type) {
                "repeat_pattern", "long_session_pattern" -> AnalysisEvidence.SOURCE_SITUATIONAL_RULE
                "numeric_ratio_pattern" -> AnalysisEvidence.SOURCE_SENTENCE_RULE
                else -> AnalysisEvidence.SOURCE_KEYWORD // general/specific - 여러 키워드 co-occurrence 조합
            }
            val label = when (rule.type) {
                "repeat_pattern" -> "반복되는 위험 신호 감지"
                "long_session_pattern" -> "장기간에 걸친 위험 신호 감지"
                "numeric_ratio_pattern" -> "비정상적인 수치 변화 감지"
                else -> "여러 위험 신호가 함께 감지됨"
            }
            AnalysisEvidence(source = source, label = label, detail = rule.condition)
        }

        return keywordEvidences + comboEvidences
    }
}
