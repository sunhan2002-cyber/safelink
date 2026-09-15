package com.safelink.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 한 문장을 말풍선 여러 개로 나눠 보내도 탐지가 무너지지 않는지.
 *
 * 규칙이 한 말풍선(턴) 안에서만 표현을 찾던 때는 회귀 문장을 어절 2개씩 나누면 위험 대화 115건 중 37건만
 * 잡혔다. 경계를 넘는 매칭([DetectionEngine] crossTurnMatches)을 넣은 뒤의 기준을 지킨다.
 */
class BubbleSplitTest {

    private val engine = Gson().let { gson ->
        DetectionEngine(
            gson.fromJson(File("src/main/assets/keyword.json").readText(), KeywordData::class.java),
            gson.fromJson(File("src/main/assets/institutions.json").readText(), InstitutionData::class.java),
            gson
        )
    }

    private fun alerted(text: String) =
        engine.analyze(ConversationTurns.split(text)).riskLevel.ordinal >= RiskLevel.WARNING.ordinal

    /** 한 메시지를 어절 [words]개씩 말풍선으로 나눈다 */
    private fun split(text: String, words: Int) = text.split('\n')
        .flatMap { line -> line.split(' ').filter { it.isNotBlank() }.chunked(words).map { it.joinToString(" ") } }
        .joinToString("\n")

    @Test
    fun `어절 2~3개씩 나눠 보내도 원래 잡던 위험 대화의 95% 이상을 잡고 일상 대화는 경고로 올리지 않는다`() {
        val cases = JsonParser.parseString(File("src/test/resources/rule_regression_cases.json").readText())
            .asJsonObject.getAsJsonArray("cases").map { it.asJsonObject }
        for (words in listOf(2, 3)) {
            val caught = cases.filter { it["kind"].asString == "danger" && alerted(it["text"].asString) }
            val stillCaught = caught.count { alerted(split(it["text"].asString, words)) }
            val benign = cases.filter { it["kind"].asString == "benign" }
            val benignAlerts = benign.filter { alerted(split(it["text"].asString, words)) }.map { it["id"].asString }
            assertTrue("어절 ${words}개씩: $stillCaught/${caught.size}", stillCaught >= caught.size * 0.95)
            assertTrue("어절 ${words}개씩 일상 오탐: $benignAlerts", benignAlerts.size <= benign.size * 0.05)
        }
    }
}
