package com.safelink.app.data.link

import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** [LinkRiskPolicy] 검증 — 악성 링크가 확인되면 전체 판정이 긴급이 되는가. */
class LinkRiskPolicyTest {

    private fun detection(level: RiskLevel, score: Int, category: String = "") = DetectionResult(
        riskLevel = level,
        score = score,
        category = category,
        originalText = "확인해 보세요 http://evil.example",
        matchedKeywords = emptyList(),
        recommendedInstitutions = emptyList()
    )

    private fun link(verdict: LinkVerdict, threat: LinkThreat? = null) =
        LinkRiskResult(ExtractedLink("http://evil.example", "http://evil.example", obfuscated = false), verdict, threat)

    @Test
    fun `위험한 링크가 없으면 판정을 바꾸지 않는다`() {
        val safe = detection(RiskLevel.SAFE, 0)
        assertSame(safe, LinkRiskPolicy.apply(safe, emptyList()))
        assertSame(safe, LinkRiskPolicy.apply(safe, listOf(link(LinkVerdict.NO_MATCH), link(LinkVerdict.UNCHECKED))))
    }

    @Test
    fun `키워드가 없어도 악성 링크가 확인되면 긴급 90점`() {
        val applied = LinkRiskPolicy.apply(
            detection(RiskLevel.SAFE, 0),
            listOf(link(LinkVerdict.DANGEROUS, LinkThreat.SOCIAL_ENGINEERING))
        )
        assertEquals(RiskLevel.CRITICAL, applied.riskLevel)
        assertEquals(LinkRiskPolicy.DANGEROUS_LINK_SCORE, applied.score)
        assertEquals("피싱·사칭 사이트", applied.category)
    }

    @Test
    fun `이미 더 높은 점수와 유형은 그대로 둔다`() {
        val applied = LinkRiskPolicy.apply(detection(RiskLevel.CRITICAL, 97, "보이스피싱"), listOf(link(LinkVerdict.DANGEROUS)))
        assertEquals(97, applied.score)
        assertEquals("보이스피싱", applied.category)
    }

    @Test
    fun `경고였던 대화도 긴급으로 올리고 유형은 유지한다`() {
        val applied = LinkRiskPolicy.apply(detection(RiskLevel.WARNING, 45, "로맨스스캠"), listOf(link(LinkVerdict.DANGEROUS)))
        assertEquals(RiskLevel.CRITICAL, applied.riskLevel)
        assertEquals(90, applied.score)
        assertEquals("로맨스스캠", applied.category)
    }

    @Test
    fun `두 번 적용해도 같다`() {
        val links = listOf(link(LinkVerdict.DANGEROUS, LinkThreat.MALWARE))
        val once = LinkRiskPolicy.apply(detection(RiskLevel.SAFE, 0), links)
        assertSame(once, LinkRiskPolicy.apply(once, links))
    }
}
