package com.safelink.app.data.model

/**
 * 위험도 4단계 (Design.md 5.1 자가진단 위험도 산출 기준)
 * - score >= 70% -> CRITICAL / >= 40% -> WARNING / >= 10% -> CAUTION / < 10% -> SAFE
 */
enum class RiskLevel(val label: String) {
    SAFE("안전"),
    CAUTION("주의"),
    WARNING("경고"),
    CRITICAL("긴급");

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score >= 66 -> CRITICAL
            score >= 31 -> WARNING
            score >= 16 -> CAUTION
            else -> SAFE
        }
    }
}
