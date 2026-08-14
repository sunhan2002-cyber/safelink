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
    }
}
