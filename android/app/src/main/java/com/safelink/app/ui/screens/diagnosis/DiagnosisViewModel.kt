package com.safelink.app.ui.screens.diagnosis

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.repository.RecordRepository
import kotlinx.coroutines.launch

/**
 * 자가진단 체크리스트 항목.
 *
 * @param text      체크리스트 화면에 표시할 문항
 * @param reason    결과 화면 "왜 이런 결과가 나왔나요?"에 표시할 문장(부드러운 어투)
 * @param highRisk  고위험 항목 여부 — 가중치 2점(일반 항목은 1점). Design.md 5.1 기준
 * @param riskTypes institutions.json risk_type_priority 키("기관사칭" 등 7분류) — 지원 탭
 *   우선순위 매칭용(Task 5.3). keyword.json의 해당 문항과 가장 가까운 중분류가
 *   subcategory_to_risk_type에서 매핑하는 위험유형을 그대로 따름(예: "가족이나 지인을
 *   사칭..."는 FM 4-1 가족사칭 진입과 같은 결로 금융사기).
 */
data class ChecklistItem(
    val text: String,
    val reason: String,
    val highRisk: Boolean,
    val riskTypes: List<String> = emptyList()
) {
    val weight: Int get() = if (highRisk) HIGH_RISK_WEIGHT else NORMAL_WEIGHT

    companion object {
        const val HIGH_RISK_WEIGHT = 2
        const val NORMAL_WEIGHT = 1
    }
}

/** 확정 문항(Task 3.6) — 가중치 기준은 Design.md 5.1, 위험유형은 Task 5.3 */
val checklistItems = listOf(
    ChecklistItem("상대방이 급하게 돈을 보내라고 요구했다", "급하게 돈을 보내라는 요구가 있었어요", true, listOf("금융사기")),
    ChecklistItem("가족이나 지인을 사칭하는 것 같은 연락을 받았다", "가족·지인을 사칭하는 것 같은 연락이 있었어요", true, listOf("금융사기")),
    ChecklistItem("협박이나 위협적인 말을 들었다", "협박이나 위협적인 말이 있었어요", true, listOf("협박")),
    ChecklistItem("개인정보나 계좌번호를 요구받았다", "개인정보·계좌번호를 요구받았어요", true, listOf("금융사기", "개인정보탈취")),
    ChecklistItem("의심스러운 링크 클릭을 유도받았다", "의심스러운 링크 클릭을 유도받았어요", false, listOf("악성링크")),
    ChecklistItem("수사기관·정부기관이라며 연락이 왔다", "수사기관을 사칭하는 연락이 있었어요", true, listOf("기관사칭")),
    ChecklistItem("높은 수익을 보장한다며 투자를 권유받았다", "높은 수익을 보장하는 투자 권유가 있었어요", false, listOf("금융사기")),
    ChecklistItem("이 일을 다른 사람에게 말하지 말라고 했다", "다른 사람에게 말하지 말라는 요구가 있었어요", true, listOf("심리조작")),
    ChecklistItem("반복적으로 연락하며 재촉당하고 있다", "반복적인 연락으로 재촉받고 있어요", false, listOf("심리조작")),
    ChecklistItem("만남이나 연락을 통제당하는 느낌이 든다", "만남이나 연락을 통제받는 느낌이 있어요", true, listOf("심리조작")),
    ChecklistItem("앱 설치나 원격 제어를 요구받았다", "앱 설치·원격 제어를 요구받았어요", false, listOf("악성링크")),
    ChecklistItem("확인하기 어려운 이야기로 불안하게 만들었다", "확인하기 어려운 이야기로 불안을 느끼고 있어요", false, listOf("심리조작")),
)

/**
 * 자가진단 산출 결과.
 *
 * @param score           0~100 백분율 점수(획득 가중치 / 전체 가중치)
 * @param level           [RiskLevel] 분류 결과
 * @param reasons         체크된 항목의 결과 문구(고위험 항목 우선 정렬)
 * @param checkedCount    체크된 항목 수
 * @param matchedRiskTypes 체크된 항목들의 위험유형 합집합(institutions.json risk_type_priority
 *   키) — 지원 탭 우선순위 매칭에 쓴다(Task 5.3, DetectionResult.recommendedInstitutions와
 *   동일한 용도).
 */
data class DiagnosisResult(
    val score: Int,
    val level: RiskLevel,
    val reasons: List<String>,
    val checkedCount: Int,
    val matchedRiskTypes: List<String> = emptyList()
)

/**
 * 자가진단 채점 로직 (Design.md 5.1) — Android 의존이 없어 순수 단위 테스트가 가능하다.
 *
 *   - 항목 가중치: 고위험 2점 / 일반 1점
 *   - score(%) = 체크된 가중치 합 / 전체 가중치 합 × 100
 *   - 70% 이상 CRITICAL / 40% 이상 WARNING / 10% 이상 CAUTION / 그 외 SAFE
 */
object DiagnosisScorer {

    /** 전체 문항 가중치 합(만점) */
    val maxWeight: Int get() = checklistItems.sumOf { it.weight }

    fun scoreOf(checkedIndices: List<Int>): Int {
        val max = maxWeight
        if (max == 0) return 0
        val got = checkedIndices.sumOf { checklistItems[it].weight }
        return (got * 100) / max
    }

    fun levelOf(score: Int): RiskLevel = when {
        score >= 70 -> RiskLevel.CRITICAL
        score >= 40 -> RiskLevel.WARNING
        score >= 10 -> RiskLevel.CAUTION
        else -> RiskLevel.SAFE
    }

    fun resultOf(checkedIndices: List<Int>): DiagnosisResult {
        val score = scoreOf(checkedIndices)
        return DiagnosisResult(
            score = score,
            level = levelOf(score),
            // 고위험 항목을 먼저 보여줘야 사용자가 무엇이 문제인지 빠르게 인지한다
            reasons = checkedIndices
                .map { checklistItems[it] }
                .sortedByDescending { it.highRisk }
                .map { it.reason },
            checkedCount = checkedIndices.size,
            matchedRiskTypes = checkedIndices.flatMap { checklistItems[it].riskTypes }.distinct()
        )
    }
}

/**
 * 자가진단 체크 상태 보관 + 위험도 산출(Task 4.9).
 *
 * 산출식은 Design.md 5.1 그대로:
 *   - 항목 가중치: 고위험 2점 / 일반 1점
 *   - score(%) = 체크된 가중치 합 / 전체 가중치 합 × 100
 *   - 70% 이상 CRITICAL / 40% 이상 WARNING / 10% 이상 CAUTION / 그 외 SAFE
 *
 * 화면 간 결과 공유를 위해 Activity 범위로 생성한다
 * ([com.safelink.app.ui.navigation.SafeLinkNavGraph] 참고).
 */
class DiagnosisViewModel(application: Application) : AndroidViewModel(application) {

    private val recordRepository: RecordRepository by lazy { RecordRepository(getApplication()) }

    /** 체크된 문항 인덱스 */
    val checkedIndices = mutableListOf<Int>().toMutableStateList()

    /** [submit] 이전에는 null — 결과 화면에 직접 진입한 경우를 구분하기 위함 */
    var result: DiagnosisResult? by mutableStateOf(null)
        private set

    fun toggle(index: Int) {
        if (index in checkedIndices) checkedIndices.remove(index) else checkedIndices.add(index)
    }

    /** 체크 상태로 점수·위험도·근거를 산출해 [result]에 보관한다. */
    fun submit(): DiagnosisResult {
        val computed = DiagnosisScorer.resultOf(checkedIndices.toList())
        result = computed

        // 검사 기록 저장 (Task 7.1) — 기기 내 DB에만 남는다.
        viewModelScope.launch {
            runCatching {
                recordRepository.saveDiagnosis(
                    score = computed.score,
                    level = computed.level,
                    reasons = computed.reasons,
                    checkedCount = computed.checkedCount
                )
            }
        }
        return computed
    }

    /** 다음 진단을 위해 체크 상태와 결과를 초기화한다. */
    fun reset() {
        checkedIndices.clear()
        result = null
    }
}
