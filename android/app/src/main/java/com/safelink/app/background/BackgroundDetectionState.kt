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

    fun update(result: DetectionResult, sourceApp: String) {
        val phrases = result.matchedKeywords
            .map { it.matchedText.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(5)

        _latestSnapshot.value = BackgroundDetectionSnapshot(
            riskLevel = result.riskLevel,
            category = result.category,
            detectedPhrases = phrases,
            sourceApp = sourceApp
        )
        _latestResult.value = result
    }
}
