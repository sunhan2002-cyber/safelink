package com.safelink.app.data.link

import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.RiskLevel

/**
 * 링크 판정을 전체 위험도에 반영하는 규칙 — 직접 분석과 백그라운드 감지가 같은 기준을 쓴다.
 *
 * 구글 차단 목록에 등재된 주소가 하나라도 있으면 그 대화는 [RiskLevel.CRITICAL] 이다. 키워드 점수처럼
 * 정황을 추정한 값이 아니라 "악성으로 확인된 주소를 받았다"는 사실이기 때문이다.
 * 예전에는 백그라운드 감지만 이렇게 처리하고, 직접 분석은 키워드가 없으면 "안전 · 0점"으로 두어
 * 바로 아래 링크 카드("피싱·사칭 사이트")와 판정이 어긋났다.
 */
object LinkRiskPolicy {

    /** 확인된 악성 링크가 있을 때의 점수. 추정이 아니라 확인된 사실이므로 긴급 구간(66~100) 안에서 높게 둔다. */
    const val DANGEROUS_LINK_SCORE = 90

    /**
     * [result] 에 링크 판정을 얹는다. 위험한 링크가 없거나 이미 반영돼 있으면 [result] 를 그대로 돌려준다.
     * 점수는 올리기만 한다 — 키워드·AI 점수가 이미 더 높으면 그 값을 둔다.
     */
    fun apply(result: DetectionResult, links: List<LinkRiskResult>): DetectionResult {
        val dangerous = links.firstOrNull { it.verdict == LinkVerdict.DANGEROUS } ?: return result
        val category = result.category.ifBlank { dangerous.threat?.label ?: "악성 링크" }
        if (result.riskLevel == RiskLevel.CRITICAL && result.score >= DANGEROUS_LINK_SCORE && result.category == category) {
            return result
        }
        return result.copy(
            riskLevel = RiskLevel.CRITICAL,
            score = maxOf(result.score, DANGEROUS_LINK_SCORE),
            category = category
        )
    }
}
