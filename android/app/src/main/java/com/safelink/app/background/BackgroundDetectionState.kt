package com.safelink.app.background

import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BackgroundDetectionSnapshot(
    val riskLevel: RiskLevel,
    val category: String,
    val detectedPhrases: List<String>,
    val sourceApp: String
)

object BackgroundDetectionState {
    private val _latestSnapshot = MutableStateFlow<BackgroundDetectionSnapshot?>(null)
    val latestSnapshot: StateFlow<BackgroundDetectionSnapshot?> = _latestSnapshot.asStateFlow()

    /**
     * 가장 최근 백그라운드 감지의 **전체 분석 결과**.
     * 스냅샷(요약)과 달리 결과 화면에서 점수·위험 유형·근거·추천 기관까지 그대로 보여주기 위해 보관한다.
     * 세션 메모리에만 유지하고 저장하지 않는다(Design.md 최소 수집 원칙).
     */
    private val _latestResult = MutableStateFlow<DetectionResult?>(null)
    val latestResult: StateFlow<DetectionResult?> = _latestResult.asStateFlow()

    /**
     * 이미 알린 건의 결과만 갈아끼운다 — AI 보조 분석이 뒤늦게 도착했을 때 쓴다.
     *
     * [update] 를 다시 부르면 감지 횟수가 한 번 더 올라가 "오늘의 알림"이 부풀기 때문에
     * 별도 함수로 뒀다. 새 사건이 아니라 같은 사건의 결과가 갱신된 것뿐이다.
     */
    fun refine(result: DetectionResult, sourceApp: String) {
        if (_latestResult.value == null) return
        _latestSnapshot.value = snapshotOf(result, sourceApp)
        _latestResult.value = result
    }

    fun update(result: DetectionResult, sourceApp: String) {
        _latestSnapshot.value = snapshotOf(result, sourceApp)
        _latestResult.value = result
    }

    /** 데이터를 모두 삭제하면 홈의 "위험 신호 감지됨" 상태도 지워 기본 홈 화면으로 돌아가게 한다. */
    fun clear() {
        _latestSnapshot.value = null
        _latestResult.value = null
    }

    private fun snapshotOf(result: DetectionResult, sourceApp: String) = BackgroundDetectionSnapshot(
        riskLevel = result.riskLevel,
        category = result.category,
        detectedPhrases = result.matchedKeywords
            .map { it.matchedText.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(5),
        sourceApp = sourceApp
    )
}
