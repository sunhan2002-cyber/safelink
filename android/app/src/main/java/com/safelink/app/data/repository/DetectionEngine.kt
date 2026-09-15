package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.link.LinkExtractor
import com.safelink.app.data.link.OfficialDomains
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
import java.util.concurrent.ConcurrentHashMap
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

    /**
     * 컴파일해 둔 정규식.
     * 예전에는 분석할 때마다 keyword.json 패턴과 콤보 규칙 패턴을 Regex 로 새로 컴파일했다.
     * 백그라운드 감지는 화면이 바뀔 때마다 분석하므로 같은 패턴을 반복 컴파일하는 비용이 그대로 쌓인다.
     * 엔진은 프로세스에서 하나만 쓰고 여러 스레드에서 부를 수 있어 ConcurrentHashMap 으로 둔다.
     */
    private val compiledPatterns = ConcurrentHashMap<String, Regex>()

    private fun regexOf(pattern: String): Regex = compiledPatterns.getOrPut(pattern) { Regex(pattern) }

    private enum class DirectRuleKind { SENTENCE, SITUATION }

    /**
     * 키워드 id와 별개로 원문 구조를 직접 판정하는 1차 규칙.
     * 단일 단어만으로는 발동하지 않도록 위험 주체·요구 행동·행동 대상이 함께 있어야 한다.
     */
    private data class DirectRiskRule(
        val id: String,
        val category: String,
        val bonus: Int,
        val kind: DirectRuleKind,
        val label: String,
        val detail: String,
        val requiredPatterns: List<Regex>
    )

    // 1차 보강 묶음 1: 보이스피싱에서 자주 함께 나타나는 문장/상황 조합.
    private val directRiskRules = listOf(
        DirectRiskRule(
            id = "DIRECT-VP-AUTH-IDENTITY-REQUEST",
            category = "보이스피싱",
            bonus = 18,
            kind = DirectRuleKind.SENTENCE,
            label = "기관 사칭 후 개인정보·인증정보 요구 감지",
            detail = "기관/수사 주체, 인증·개인정보, 제출·전송 요청이 함께 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:금융감독원|금감원|검찰|경찰|수사관|은행\\s*직원|카드사\\s*직원)"),
                Regex("(?:인증번호|보안카드|주민등록번호|신분증\\s*(?:사진)?|개인정보|본인\\s*인증|본인\\s*확인)"),
                Regex("(?:알려(?:주세요|주셔야|줘)|입력(?:하세요|해(?:주세요|주셔야|줘))|제출(?:해주세요|하셔야|해)|전송(?:해주세요|하셔야|해))")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-VP-URGENT-TRANSFER",
            category = "보이스피싱",
            bonus = 18,
            kind = DirectRuleKind.SITUATION,
            label = "긴급성 유도와 송금 요구 조합 감지",
            detail = "즉시 처리 압박과 금전 이체 요청이 함께 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:지금\\s*(?:당장|바로)|오늘\\s*안(?:에|으로)|즉시|지체\\s*없이)"),
                Regex("(?:송금|이체|입금|계좌(?:로|번호)|돈을?\\s*보내)"),
                Regex("(?:해(?:주세요|주셔야|야|줘)|하세요|부탁(?:드립니다|해요)?|보내(?:주세요|주셔야|야|줘))")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-VP-LINK-INSTALL-VERIFY",
            category = "보이스피싱",
            bonus = 20,
            kind = DirectRuleKind.SENTENCE,
            label = "링크·앱 설치를 통한 인증 유도 감지",
            detail = "링크 또는 앱 설치 후 인증·확인을 진행하라는 요청이 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:링크|URL|앱\\s*설치|어플\\s*설치|APK|원격\\s*제어)", RegexOption.IGNORE_CASE),
                Regex("(?:인증|본인\\s*확인|보안\\s*확인|확인\\s*절차)"),
                Regex("(?:눌러|설치해|접속해|입력해|진행해)(?:주세요|주셔야|야|줘|하세요)?")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-VP-SECRET-MONEY",
            category = "보이스피싱",
            bonus = 20,
            kind = DirectRuleKind.SITUATION,
            label = "비밀 유지와 금전 요구 조합 감지",
            detail = "주변에 알리지 말라는 요구와 송금·입금 요청이 함께 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:아무(?:한테|에게)도\\s*말하지|비밀로|혼자만\\s*알고)"),
                Regex("(?:송금|이체|입금|계좌(?:로|번호)|돈을?\\s*보내)"),
                Regex("(?:해(?:주세요|주셔야|야|줘)|하세요|부탁(?:드립니다|해요)?|보내(?:주세요|주셔야|야|줘))")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-FM-NEW-NUMBER-MONEY",
            category = "가족사칭",
            bonus = 20,
            kind = DirectRuleKind.SITUATION,
            label = "가족 사칭 후 연락 회피·금전 요구 감지",
            detail = "가족 관계 사칭, 새 번호·기기 문제, 통화 회피, 금전 요청이 함께 확인되었습니다.",
            // 네 조건이 모두 있어야 발동한다(금전 요구 없이 폰 고장·통화 회피만으로는 가산하지 않음).
            // 조건 1~3은 신기훈 탐지보강 2-3, 조건 2·4의 추가 표현은 김선한 구현작업 v1 2단계 기준으로 넓혔다.
            requiredPatterns = listOf(
                Regex("(?:엄마|아빠|아들|딸|누나|언니|오빠|형)\\s*(?:나야|나예요|나에요|이야|이에요|인데)"),
                Regex("(?:(?:휴대|핸드)?폰\\s*(?:이|은|도)?\\s*(?:고장|깨져|깨졌|분실|잃어버|먹통)|새\\s*번호|번호(?:가|를)?\\s*(?:바뀌|바꿨)|임시\\s*폰|친구\\s*폰|PC\\s*카톡)", RegexOption.IGNORE_CASE),
                Regex("(?:통화|전화)\\s*(?:가|는|도)?\\s*(?:안\\s*(?:돼|되|됨)|못\\s*(?:해|하)|어려워)"),
                Regex("(?:송금|이체|입금|계좌(?:로|번호)|돈을?\\s*보내|대신\\s*보내|수리비|병원비|합의금)")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-RS-TRUST-EMERGENCY-MONEY",
            category = "로맨스스캠",
            bonus = 20,
            kind = DirectRuleKind.SITUATION,
            label = "관계 신뢰 형성 뒤 긴급 금전 요구 감지",
            detail = "친밀감·미래 약속 표현 뒤 경제적 위기와 금전 요청이 함께 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:사랑해|자기야|결혼(?:하고|하자|할까)|함께\\s*살|평생)"),
                Regex("(?:사업\\s*자금|병원비|사고가\\s*났|급한\\s*돈|경제적으로\\s*힘들)"),
                Regex("(?:송금|이체|입금|계좌(?:로|번호)|도와(?:줄|줘)|돈을?\\s*보내)")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-IV-GUARANTEED-RETURN-DEPOSIT",
            category = "투자사기",
            bonus = 20,
            kind = DirectRuleKind.SITUATION,
            label = "수익 보장과 투자금 입금 유도 감지",
            detail = "손실 없는 수익 약속, 투자 대상, 입금 요청이 함께 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:원금\\s*보장|확정\\s*수익|무조건\\s*수익|손실\\s*없)"),
                Regex("(?:투자|코인|주식|VIP\\s*방)", RegexOption.IGNORE_CASE),
                Regex("(?:송금|이체|입금|계좌(?:로|번호)|돈을?\\s*보내)")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-TH-EXPOSURE-MONEY-DEMAND",
            category = "협박·갈취",
            bonus = 24,
            kind = DirectRuleKind.SITUATION,
            label = "유포 위협과 금전 요구 조합 감지",
            detail = "개인 정보·영상 등의 유포 위협과 금전 요구가 함께 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:유포|뿌리|공개|가족에게\\s*알리|영상)"),
                Regex("(?:송금|이체|입금|계좌(?:로|번호)|돈을?\\s*보내)"),
                Regex("(?:안\\s*하면|않으면|전에|지금\\s*(?:당장|바로)|즉시)")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-TH-SEXTORTION",
            category = "협박·갈취",
            bonus = 24,
            kind = DirectRuleKind.SITUATION,
            label = "영상통화 유도 후 유포 위협 조합 감지",
            detail = "영상통화·앱 설치 유도와 촬영물 유포 위협이 함께 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:영상\\s*통화|화상\\s*통화|영상통화|얼굴\\s*보고)"),
                Regex("(?:앱\\s*설치|어플\\s*설치|APK|권한\\s*(?:허용|승인)|연락처\\s*(?:동기화|접근))", RegexOption.IGNORE_CASE),
                Regex("(?:녹화|촬영(?:본|물)?|영상).{0,20}(?:유포|뿌리|보낸다|올린|퍼뜨)|(?:유포|뿌리|보낸다|올린).{0,20}(?:녹화|영상)")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-GL-DENY-BLAME",
            category = "가스라이팅",
            bonus = 16,
            kind = DirectRuleKind.SENTENCE,
            label = "기억 부정과 책임 전가 조합 감지",
            detail = "상대의 기억·인식을 부정하면서 책임을 전가하는 표현이 함께 확인되었습니다.",
            requiredPatterns = listOf(
                Regex("(?:내가\\s*언제|그런\\s*적\\s*없|네가\\s*잘못\\s*기억)"),
                Regex("(?:너\\s*(?:때문|탓)|네\\s*탓|과민반응|예민해서)")
            )
        ),
        DirectRiskRule(
            id = "DIRECT-GL-ISOLATION-CONTROL",
            category = "가스라이팅",
            bonus = 20,
            kind = DirectRuleKind.SITUATION,
            label = "관계 고립과 통제 조합 감지",
            detail = "주변 관계를 끊게 하거나 상대 의존을 강요하는 표현이 함께 확인되었습니다.",
            requiredPatterns = listOf(
                // 턴 구분자가 줄바꿈이라 점(.)으로는 줄을 넘어가지 못한다 — 두 턴에 걸쳐 나와도 잡히게 둔다
                Regex("(?:친구|가족|걔)[\\s\\S]{0,12}(?:만나지|연락하지|거리\\s*둬)"),
                Regex("(?:나\\s*아니면\\s*안\\s*돼|내\\s*말만\\s*들어|내가\\s*없으면)")
            )
        )
    )

    /** 원문 텍스트 1건을 분석해서 [DetectionResult]로 변환. 화면/ViewModel에서 사용하는 기본 진입점. */
    fun analyze(originalText: String): DetectionResult = analyze(listOf(originalText))

    /** 여러 턴(대화)을 이어서 분석. 콤보 판정 등 세션 단위 로직 검증에 사용. */
    fun analyze(turns: List<String>): DetectionResult {
        // 줄바꿈으로 합친다 — 붙여넣은 대화의 줄 구분이 화면에서도 그대로 보이고,
        // 구분자가 한 글자라 매칭 위치(startIndex/endIndex) 계산은 공백일 때와 같다.
        val originalText = turns.joinToString("\n")
        val turnOffsets = turnOffsets(turns)

        val rawMatches = turns.flatMapIndexed { turnIndex, turnText ->
            matchKeywordsInTurn(turnText, turnIndex, turnOffsets[turnIndex])
        }

        val suppressed = findSuppressedByOverlap(rawMatches)
        val (baseScore, matchedKeywords) = scoreMatches(rawMatches, suppressed)
        val comboIds = evaluateComboRules(rawMatches, suppressed, turnCount = turns.size, originalText = originalText)
        val comboBonus = comboIds.sumOf { id -> keywordData.comboBonusRules.first { it.id == id }.bonus }
        val directRules = evaluateDirectRiskRules(originalText)
        val directBonus = directRules.sumOf { it.bonus }
        val totalScore = min(baseScore + comboBonus + directBonus, 100.0)
        val score = totalScore.toInt()

        val riskLevel = RiskLevel.fromScore(score)
        val category = rawMatches.groupBy { it.entry.category }
            .maxByOrNull { (_, group) -> group.sumOf { it.entry.weight } }
            ?.key ?: directRules.firstOrNull()?.category.orEmpty()

        val recommendedInstitutions = resolveInstitutions(rawMatches, comboIds, score)
        val (existingSentenceEvidences, existingSituationalEvidences) = buildRuleEvidences(rawMatches, suppressed, comboIds)
        val sentenceRuleEvidences = existingSentenceEvidences + directRules
            .filter { it.kind == DirectRuleKind.SENTENCE }
            .map { AnalysisEvidence(it.label, "${it.detail} (+${it.bonus}점)") }
        val situationalRuleEvidences = existingSituationalEvidences + directRules
            .filter { it.kind == DirectRuleKind.SITUATION }
            .map { AnalysisEvidence(it.label, "${it.detail} (+${it.bonus}점)") }

        return DetectionResult(
            riskLevel = riskLevel,
            score = score,
            category = category,
            originalText = originalText,
            matchedKeywords = matchedKeywords,
            recommendedInstitutions = recommendedInstitutions,
            appliedComboIds = comboIds,
            appliedDirectRuleIds = directRules.map { it.id },
            sentenceRuleEvidences = sentenceRuleEvidences,
            situationalRuleEvidences = situationalRuleEvidences
        )
    }

    private fun evaluateDirectRiskRules(originalText: String): List<DirectRiskRule> =
        directRiskRules.filter { rule -> rule.requiredPatterns.all { it.containsMatchIn(originalText) } }

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
            if (i < turns.size - 1) pos += 1 // 턴 구분자(줄바꿈 한 글자)
        }
        return offsets
    }

    private fun matchKeywordsInTurn(turnText: String, turnIndex: Int, turnOffset: Int): List<RawMatch> {
        val matches = mutableListOf<RawMatch>()
        val compactTurn = CompactText.of(turnText)
        for (entry in keywordData.keywords) {
            when (entry.matchType) {
                "keyword" -> {
                    val needle = entry.keyword ?: continue
                    // 띄어쓰기 무시 비교 (신기훈 탐지보강 2-1): 키워드와 원문 양쪽의 공백을 빼고 비교한다.
                    // "대신송금해줄래"가 "대신 송금"을, "통화가안돼"가 "지금 통화가 안 돼"의 뒷부분을 잡지 못하던 문제.
                    // 공백을 빼면 2글자 이하가 되는 짧은 키워드(검사·즉시·압류·구속·옷 벗)는 옆 단어와 우연히 붙어
                    // 잡힐 위험이 커서 예전처럼 글자 그대로 일치할 때만 잡는다.
                    // 밑줄 위치와 matchedText 는 띄어쓰기가 그대로인 원문 기준으로 되돌린다.
                    val compactNeedle = CompactText.strip(needle)
                    if (compactNeedle.length < MIN_SPACE_INSENSITIVE_LENGTH) {
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
                    } else {
                        compactTurn.findAll(compactNeedle).forEach { range ->
                            matches += RawMatch(
                                entry = entry,
                                turnIndex = turnIndex,
                                startInFull = turnOffset + range.first,
                                endInFull = turnOffset + range.last + 1,
                                matchedText = turnText.substring(range.first, range.last + 1)
                            )
                        }
                    }
                }
                "regex-simple" -> {
                    // 링크는 정규식 대신 링크 검사와 같은 추출기로 찾는다. 정규식(https?://)은 scheme 이 붙은 주소만 잡아,
                    // "대장방문.com/6ITtt", "bit.ly/x", "evil[.]com" 처럼 scheme 없이·훼손해서 온 링크는 스미싱 조합에서 빠졌다.
                    // 택배사·정부 공식 주소는 의심 링크로 세지 않는다(OfficialDomains) — 진짜 택배 안내가 스미싱 조합으로
                    // 경고가 뜨고, 결과 화면에서 정상 주소가 위험 표현으로 강조되던 문제. 흉내 낸 주소는 걸러지지 않는다.
                    if (entry.id == URL_KEYWORD_ID) {
                        var searchFrom = 0
                        for (link in LinkExtractor.extract(turnText)) {
                            val idx = turnText.indexOf(link.displayText, searchFrom).takeIf { it >= 0 } ?: continue
                            searchFrom = idx + link.displayText.length
                            if (OfficialDomains.isOfficialUrl(link.url)) continue
                            matches += RawMatch(
                                entry = entry,
                                turnIndex = turnIndex,
                                startInFull = turnOffset + idx,
                                endInFull = turnOffset + idx + link.displayText.length,
                                matchedText = link.displayText
                            )
                        }
                        continue
                    }
                    val pattern = entry.pattern ?: continue
                    regexOf(pattern).findAll(turnText).forEach { m ->
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
                    regexOf(pattern).findAll(turnText).forEach { m ->
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
                // 점수가 없는 표시용 매칭(링크 자체 VP-1-6-004 등, weight 0)은 안에 든 다른 신호를 가리지 않는다.
                // 링크 전체("hxxp://evil[.]top/k2")가 잡히면서 그 안의 "일부러 망가뜨린 링크" 신호(hxxp://, [.])가
                // 짧다는 이유로 점수에서 빠져 스미싱 문자를 놓치던 문제.
                if (b.entry.weight == 0 || b.entry.structuralOnly) continue
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
        // 같은 유형 안에서 서로 다른 위험 행동이 몇 가지 함께 나왔는가. 행동 하나는 일상 대화에도 나오지만
        // (폰 고장, 송금 얘기), 서로 다른 행동이 겹칠수록 위험 흐름일 가능성이 커진다.
        // 2개(COMBO-GENERAL-2CAT)는 맨 끝에서 판단한다 — 같은 두 행동을 이미 전용 조합 규칙이 잡았으면 중복 가산하지 않기 위해.
        when {
            maxDistinct >= 4 -> triggered += "COMBO-GENERAL-4CAT"
            maxDistinct >= 3 -> triggered += "COMBO-GENERAL-3CAT"
        }

        if ("VP-1-3-003" in matchedIds && ("VP-1-3-001" in matchedIds || "VP-1-3-002" in matchedIds)) {
            triggered += "COMBO-VP-PHONE-VERIFY"
        }
        if (URL_KEYWORD_ID in matchedIds &&
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
                val match = regexOf(rule.pattern!!).find(originalText)
                val before = match?.groups?.get(1)?.value?.toDoubleOrNull()
                val after = match?.groups?.get(2)?.value?.toDoubleOrNull()
                if (before != null && after != null && before > 0) {
                    val growthRatePercent = (after - before) / before * 100.0
                    if (growthRatePercent >= rule.minGrowthRatePercent!!) triggered += rule.id
                }
            }

        // 서로 다른 위험 행동 2개: 전용 조합 규칙(특정 두 행동의 조합)이 하나도 발동하지 않았을 때만 가산한다.
        if (maxDistinct == 2 && triggered.isEmpty()) {
            triggered += "COMBO-GENERAL-2CAT"
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
        /**
         * 띄어쓰기 무시 비교를 적용할 최소 길이(공백을 뺀 키워드 글자 수). 이보다 짧으면 글자 그대로 일치만 본다.
         * 2글자 키워드(검사·즉시·압류·구속·옷 벗)는 "검사결과"·"즉시불" 처럼 다른 단어 안에서 우연히 잡히기 쉽다.
         */
        internal const val MIN_SPACE_INSENSITIVE_LENGTH = 3

        /** 원문 속 링크(https?://...)를 잡는 키워드 id. 스미싱 조합(COMBO-VP-SMISHING)의 링크 조건이다. */
        private const val URL_KEYWORD_ID = "VP-1-6-004"

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

        // AI 근거는 sentenceRuleEvidences/situationalRuleEvidences에 안 섞는다 - AI 보조분석
        // 근거는 이미 aiSummary/aiDetectedPattern이라는 전용 필드가 있다(6주차 구조 개선,
        // AnalysisEvidence.kt 참고). 문장/상황 규칙 근거는 온디바이스 analyze() 결과 그대로 유지.
        return original.copy(
            score = adjustedScore,
            riskLevel = RiskLevel.fromScore(adjustedScore),
            recommendedInstitutions = mergeInstitutions(original.recommendedInstitutions, response),
            aiSummary = response.contextAnalysisSummary,
            aiDetectedPattern = response.contextDetectedPattern
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
    // 6. 분석 근거 정리 (5주차 신설, 6주차 구조 개선 - 결과 화면에 "왜 이 점수인지" 넘길
    //    데이터를 문장 규칙/상황 규칙 전용 필드로 직접 분리. AnalysisEvidence 참고.)
    //
    // 키워드 매칭(match_type: keyword/regex-simple)과 키워드 co-occurrence 콤보(general/
    // specific)는 여기서 안 다룬다 - matchedKeywords/appliedComboIds가 이미 그 근거다.
    // 여기서는 "문장 규칙"(regex-complex 매칭, numeric_ratio_pattern 콤보)과 "상황 규칙"
    // (repeat_pattern/long_session_pattern 콤보)만 뽑아서 전용 리스트 2개로 분리한다.
    // AI 근거는 여기서 안 만듦 - aiSummary/aiDetectedPattern이 이미 전용 필드다.
    // ─────────────────────────────────────────────────────────────────

    private fun buildRuleEvidences(
        rawMatches: List<RawMatch>,
        suppressed: Set<RawMatch>,
        comboIds: List<String>
    ): Pair<List<AnalysisEvidence>, List<AnalysisEvidence>> {
        val sentenceRuleFromKeywords = rawMatches
            .filterNot { it in suppressed }
            .filter { it.entry.matchType == "regex-complex" }
            .distinctBy { it.entry.id }
            .map { m -> AnalysisEvidence(label = m.entry.description, detail = m.matchedText) }

        val triggeredRules = comboIds.mapNotNull { id -> keywordData.comboBonusRules.firstOrNull { it.id == id } }

        val sentenceRuleFromCombos = triggeredRules
            .filter { it.type == "numeric_ratio_pattern" }
            .map { rule -> AnalysisEvidence(label = "비정상적인 수치 변화 감지", detail = rule.condition) }

        val situationalRuleEvidences = triggeredRules
            .filter { it.type == "repeat_pattern" || it.type == "long_session_pattern" }
            .map { rule ->
                val label = if (rule.type == "repeat_pattern") "반복되는 위험 신호 감지" else "장기간에 걸친 위험 신호 감지"
                AnalysisEvidence(label = label, detail = rule.condition)
            }

        return (sentenceRuleFromKeywords + sentenceRuleFromCombos) to situationalRuleEvidences
    }
}
/**
 * 공백(스페이스·탭·줄바꿈 없는 공백 문자)을 뺀 문자열과, 뺀 문자열의 각 글자가 원문 어디였는지를 함께 들고 있다.
 * 줄바꿈은 빼지 않는다 — 메시지 경계를 넘어 두 말풍선의 글자가 이어져 잡히면 안 되기 때문이다.
 */
internal class CompactText private constructor(private val compact: String, private val originalIndex: IntArray) {

    /** [compactNeedle](공백 없는 키워드)이 나오는 원문 구간들. 한 번 잡힌 구간 뒤부터 다시 찾는다. */
    fun findAll(compactNeedle: String): List<IntRange> {
        if (compactNeedle.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var from = 0
        while (true) {
            val idx = compact.indexOf(compactNeedle, from)
            if (idx < 0) break
            ranges += originalIndex[idx]..originalIndex[idx + compactNeedle.length - 1]
            from = idx + compactNeedle.length
        }
        return ranges
    }

    companion object {
        private fun isSpace(c: Char) = c == ' ' || c == '	' || c == ' ' || c == '　'

        fun strip(text: String): String = text.filterNot(::isSpace)

        fun of(text: String): CompactText {
            val sb = StringBuilder(text.length)
            val index = IntArray(text.length)
            var n = 0
            text.forEachIndexed { i, c ->
                if (!isSpace(c)) {
                    sb.append(c)
                    index[n++] = i
                }
            }
            return CompactText(sb.toString(), index.copyOf(n))
        }
    }
}
