package com.safelink.app.ui.screens.detection

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.MatchedKeyword
import com.safelink.app.data.model.RecommendedInstitutionUi
import com.safelink.app.data.model.RiskLevel
import org.json.JSONObject

/**
 * 대화 분석 공유 ViewModel — DetectionInput -> Analyzing -> DetectionResult 세 화면이
 * NavGraph 범위에서 같은 인스턴스를 공유한다 (docs/AndroidStructure.md 인자 전달 규칙:
 * 복합 객체는 라우트로 넘기지 않고 공유 ViewModel로 전달).
 *
 * 매칭 로직(1차 단순 버전) — assets/keyword.json(data/keyword.json 복사본, 80개 항목) 사용:
 *  - match_type == "keyword" 항목의 keyword 문자열 포함 여부 검사, 첫 등장 위치 기록
 *  - 점수 = 매칭 항목 weight 합 (cap 100), 위험도 = RiskLevel.fromScore(score)
 *  - 카테고리 = 매칭 가중치 합이 가장 큰 카테고리
 *  - TODO(신기훈 로직으로 교체): 반복 감쇠(repeat_decay), 조합 보너스(combo_bonus_rules),
 *    regex 매칭(match_type == "regex"), 세션 누적 점수
 *
 * 추천 기관(1차 규칙): 점수 31점(중간 구간) 이상일 때 카테고리별 기본 기관 노출
 *  - TODO(신기훈): institutions.json 의 subcategory_to_risk_type + standalone_recommend
 *    게이팅 규칙으로 교체
 *
 * 원문(originalText)은 세션 메모리에만 유지하고 저장하지 않는다 (Design.md 최소 수집 원칙).
 */
class DetectionViewModel(application: Application) : AndroidViewModel(application) {

    /** 원문 텍스트 — 용어 통일본(김선한 03) 기준 이번 주 핵심 입력값 */
    var originalText by mutableStateOf("")

    /** 입력 방식 — "텍스트 입력" | "스크린샷 업로드" (통일본 inputMethod) */
    var inputMethod by mutableStateOf("텍스트 입력")

    var result by mutableStateOf<DetectionResult?>(null)
        private set

    private data class KeywordEntry(
        val id: String,
        val category: String,
        val subcategoryId: String,
        val subcategoryName: String,
        val keyword: String,
        val weight: Int,
        val description: String
    )

    private val keywords: List<KeywordEntry> by lazy { loadKeywords() }

    private fun loadKeywords(): List<KeywordEntry> {
        return runCatching {
            val json = getApplication<Application>().assets
                .open("keyword.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(json).getJSONArray("keywords")
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (o.optString("match_type") != "keyword") continue // TODO: regex 지원
                    add(
                        KeywordEntry(
                            id = o.getString("id"),
                            category = o.getString("category"),
                            subcategoryId = o.getString("subcategory_id"),
                            subcategoryName = o.getString("subcategory"),
                            keyword = o.getString("keyword"),
                            weight = o.getInt("weight"),
                            description = o.optString("description")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /** 원문 텍스트를 분석해 result 에 반영한다. Analyzing 화면 진입 전에 호출. */
    fun analyze() {
        val text = originalText
        val matched = mutableListOf<MatchedKeyword>()

        keywords.forEach { entry ->
            val index = text.indexOf(entry.keyword)
            if (index >= 0) {
                matched += MatchedKeyword(
                    keywordId = entry.id,
                    subcategoryId = entry.subcategoryId,
                    subcategoryName = entry.subcategoryName,
                    matchedText = entry.keyword,
                    startIndex = index,
                    endIndex = index + entry.keyword.length,
                    weight = entry.weight,
                    description = entry.description
                )
            }
        }

        val score = matched.sumOf { it.weight }.coerceAtMost(100)
        val riskLevel = RiskLevel.fromScore(score)
        val category = matched
            .groupBy { kw -> keywords.first { it.id == kw.keywordId }.category }
            .maxByOrNull { (_, list) -> list.sumOf { it.weight } }
            ?.key ?: ""

        result = DetectionResult(
            riskLevel = riskLevel,
            score = score,
            category = category,
            originalText = text,
            matchedKeywords = matched.sortedBy { it.startIndex },
            recommendedInstitutions = recommendInstitutions(category, score)
        )
    }

    /**
     * 추천 기관 v1 규칙 — 점수 31점(중간) 이상일 때 카테고리 기본 기관 노출.
     * 기관 정보는 data/institutions.json 실제 항목 기준.
     */
    private fun recommendInstitutions(category: String, score: Int): List<RecommendedInstitutionUi> {
        if (score < 31) return emptyList()
        return when (category) {
            "보이스피싱" -> listOf(
                RecommendedInstitutionUi("GOV-POLICE", "경찰청 (사이버수사대)", "112", 1, "기관사칭 및 범죄 신고, 수사 요청 가능", "기관사칭", "긴급대응"),
                RecommendedInstitutionUi("GOV-FSS", "금융감독원 (금융사기대응단)", "1332", 2, "금융사기 피해 상담과 지급정지 안내 가능", "금융사기", "긴급대응"),
                RecommendedInstitutionUi("PUB-LEGALAID", "대한법률구조공단", "132", 3, "피해 회복을 위한 무료 법률 상담 가능", "금융사기", "법률·행정지원")
            )
            "로맨스스캠" -> listOf(
                RecommendedInstitutionUi("GOV-FSS", "금융감독원 (금융사기대응단)", "1332", 1, "금융사기 피해 상담 및 지급정지 가능", "금융사기", "긴급대응"),
                RecommendedInstitutionUi("GOV-POLICE", "경찰청 (사이버수사대)", "112", 2, "사기 피해 신고 및 수사 요청 가능", "금융사기", "긴급대응"),
                RecommendedInstitutionUi("PUB-MENTALHEALTH", "한국심리학회·지역 정신건강복지센터", "지역별 센터", 3, "심리적 조작 피해 상담 및 회복 지원", "심리조작", "상담")
            )
            "가스라이팅" -> listOf(
                RecommendedInstitutionUi("PUB-WOMEN1366", "여성가족부·한국여성인권진흥원", "1366", 1, "관계 내 조작·통제 피해자 상담 및 보호", "심리조작", "상담"),
                RecommendedInstitutionUi("PUB-MENTALHEALTH", "한국심리학회·지역 정신건강복지센터", "지역별 센터", 2, "심리적 조작·가스라이팅 전문 상담 및 정신건강 지원", "심리조작", "상담")
            )
            else -> emptyList()
        }
    }
}
