package com.safelink.app.ui.screens.record

import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.repository.RecordItem
import com.safelink.app.data.repository.RecordType

/** 기록 화면 보기 방식 */
enum class RecordViewMode(val label: String) {
    /** 모든 기록을 최신순으로 */
    LATEST("최신순"),

    /** 같은 상황(위험 유형)끼리 묶어서 */
    BY_SITUATION("상황별")
}

/**
 * 같은 상황으로 묶인 기록 묶음.
 *
 * @param key 묶음을 구분하는 값 — 펼침 상태를 기억할 때 쓴다
 * @param records 최신순
 */
data class RecordGroup(
    val key: String,
    val title: String,
    val records: List<RecordItem>
) {
    /** 묶음 안에서 가장 높은 위험도 — 묶음 헤더의 배지와 정렬 기준 */
    val highestRisk: RiskLevel = records.maxOfOrNull { it.riskLevel } ?: RiskLevel.SAFE

    val latestTimestamp: Long = records.maxOfOrNull { it.timestamp } ?: 0L

    /** 위험도별 건수(높은 위험도부터, 0건은 제외) — "긴급 2 · 경고 1" 표시용 */
    val countsByRisk: List<Pair<RiskLevel, Int>> =
        RiskLevel.entries.reversed()
            .map { level -> level to records.count { it.riskLevel == level } }
            .filter { it.second > 0 }
}

/**
 * 기록을 상황(위험 유형)별로 묶는다.
 *
 * ── 무엇을 "같은 상황"으로 보는가 ─────────────────────────────────────
 * 대화 분석 기록은 판정 당시의 위험 유형(보이스피싱, 협박·갈취 …)이 저장돼 있어 그 값으로 묶는다.
 * 새로 저장할 것 없이 이미 있는 값만 쓰므로 예전 기록도 그대로 묶인다.
 *
 * 안전으로 판정된 검사와 자가진단은 수법이 아니라서 각각 따로 모은다 — 보이스피싱 묶음 사이에 섞이면
 * "이 수법에 몇 번 노출됐나"가 흐려진다. 규칙 엔진은 점수가 낮아도 가장 많이 맞은 유형을 category 에
 * 채우므로(안전 판정인데 "보이스피싱"), 유형이 비었는지가 아니라 **판정이 안전인지**로 가른다.
 *
 * ── 순서 ─────────────────────────────────────────────────────────────
 * 가장 위험했던 묶음이 먼저, 같으면 최근에 기록된 묶음이 먼저.
 * 사용자가 이 화면에서 제일 먼저 알아야 하는 건 "가장 심각하게, 가장 최근에 겪은 상황"이다.
 */
fun groupBySituation(records: List<RecordItem>): List<RecordGroup> =
    records
        .groupBy { situationKey(it) }
        .map { (key, items) ->
            RecordGroup(
                key = key,
                title = situationTitle(key),
                records = items.sortedByDescending { it.timestamp }
            )
        }
        .sortedWith(
            compareByDescending<RecordGroup> { it.highestRisk }
                .thenByDescending { it.latestTimestamp }
        )

internal const val GROUP_KEY_NO_SIGNAL = "__no_signal__"
internal const val GROUP_KEY_DIAGNOSIS = "__diagnosis__"

private fun situationKey(record: RecordItem): String = when {
    record.type == RecordType.DIAGNOSIS -> GROUP_KEY_DIAGNOSIS
    record.riskLevel == RiskLevel.SAFE || record.category.isBlank() -> GROUP_KEY_NO_SIGNAL
    else -> record.category
}

private fun situationTitle(key: String): String = when (key) {
    GROUP_KEY_DIAGNOSIS -> "자가진단"
    GROUP_KEY_NO_SIGNAL -> "안전 판정"
    else -> key
}
