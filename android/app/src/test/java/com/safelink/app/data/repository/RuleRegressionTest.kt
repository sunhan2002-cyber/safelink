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
 * 규칙 엔진 회귀 테스트 — "위험 대화를 몇 % 잡고, 일상 대화를 몇 % 잘못 잡는가".
 *
 * 문장 묶음은 `src/test/resources/rule_regression_cases.json` 에 있다(작성 규칙은 그 파일의 _meta 참고).
 * 판정은 실제 앱이 쓰는 키워드 파일(`src/main/assets`)로 한다 — 테스트용 사본이 뒤처져도 앱 기준으로 잰다.
 *
 * 결과표는 표준 출력과 `build/reports/rule-regression/report.md` 에 남는다.
 * 규칙을 바꾸기 전후로 이 표를 비교하면 된다.
 *
 * ── 통과 기준 ─────────────────────────────────────────────────────────
 * - 실제로 놓쳤던 대화(source=real-failure)는 전부 경고 이상이어야 한다 — 같은 실수를 다시 하지 않기 위해.
 * - 위험 대화 탐지율, 일상 대화 오탐률이 아래 기준을 넘어야 한다. 기준은 측정해서 정한 값이고,
 *   규칙을 고쳐 더 좋아지면 올린다(내리지 않는다).
 */
class RuleRegressionTest {

    private data class Case(
        val id: String,
        val category: String,
        val kind: String,
        val author: String,
        val source: String,
        val text: String
    )

    private data class Outcome(val case: Case, val level: RiskLevel, val score: Int, val matched: List<String>, val directRules: List<String>, val combos: List<String>) {
        val alerted: Boolean get() = level.ordinal >= RiskLevel.WARNING.ordinal
    }

    private fun engine(): DetectionEngine {
        val gson = Gson()
        val keywordData = gson.fromJson(File("src/main/assets/keyword.json").readText(), KeywordData::class.java)
        val institutionData = gson.fromJson(File("src/main/assets/institutions.json").readText(), InstitutionData::class.java)
        return DetectionEngine(keywordData, institutionData, gson)
    }

    private fun loadCases(): List<Case> {
        val json = javaClass.classLoader!!.getResourceAsStream("rule_regression_cases.json")!!.bufferedReader().readText()
        return JsonParser.parseString(json).asJsonObject.getAsJsonArray("cases").map { element ->
            val o = element.asJsonObject
            Case(
                id = o["id"].asString,
                category = o["category"].asString,
                kind = o["kind"].asString,
                author = o["author"].asString,
                source = o["source"].asString,
                text = o["text"].asString
            )
        }
    }

    @Test
    fun `규칙 엔진 회귀 측정`() {
        val engine = engine()
        val outcomes = loadCases().map { c ->
            val r = engine.analyze(ConversationTurns.split(c.text))
            Outcome(c, r.riskLevel, r.score, r.matchedKeywords.map { it.matchedText }.distinct(), r.appliedDirectRuleIds, r.appliedComboIds)
        }

        // 개발용(규칙을 고칠 때 보는 문장)과 확인용(holdout, 규칙을 고칠 때 보지 않는 문장)을 따로 잰다.
        // 확인용 점수가 개발용보다 크게 낮으면 문장 묶음에 규칙을 끼워 맞춘 것(과적합)이다.
        val dev = outcomes.filter { it.case.source != "holdout" }
        val holdout = outcomes.filter { it.case.source == "holdout" }
        val danger = dev.filter { it.case.kind == "danger" }
        val benign = dev.filter { it.case.kind == "benign" }
        val recall = danger.count { it.alerted }.toDouble() / danger.size
        val falsePositive = benign.count { it.alerted }.toDouble() / benign.size
        val hDanger = holdout.filter { it.case.kind == "danger" }
        val hBenign = holdout.filter { it.case.kind == "benign" }

        val report = buildString {
            appendLine("# 규칙 엔진 회귀 측정")
            appendLine()
            appendLine("| 구분 | 개발용 | 확인용(holdout) |")
            appendLine("|---|---|---|")
            appendLine("| 위험 대화 탐지율 (경고 이상) | ${pct(recall)} (${danger.count { it.alerted }}/${danger.size}) | ${ratio(hDanger.count { it.alerted }, hDanger.size)} |")
            appendLine("| 일상 대화 오탐률 (경고 이상) | ${pct(falsePositive)} (${benign.count { it.alerted }}/${benign.size}) | ${ratio(hBenign.count { it.alerted }, hBenign.size)} |")
            appendLine()
            val ambiguousCritical = dev.filter { it.case.kind == "ambiguous" && it.level == RiskLevel.CRITICAL }
            appendLine("| 애매한 대화가 긴급으로 뜬 수 | ${ambiguousCritical.size}건${if (ambiguousCritical.isEmpty()) "" else " (" + ambiguousCritical.joinToString { it.case.id } + ")"} | |")
            appendLine()
            appendLine("## 유형별 (개발용)")
            appendLine()
            appendLine("| 유형 | 위험 탐지 | 일상 오탐 | 애매(점수) |")
            appendLine("|---|---|---|---|")
            dev.groupBy { it.case.category }.forEach { (category, list) ->
                val d = list.filter { it.case.kind == "danger" }
                val b = list.filter { it.case.kind == "benign" }
                val a = list.filter { it.case.kind == "ambiguous" }
                appendLine("| $category | ${d.count { it.alerted }}/${d.size} | ${b.count { it.alerted }}/${b.size} | ${a.joinToString(", ") { it.score.toString() }} |")
            }
            appendLine()
            appendLine("## 사례별 (개발용)")
            caseTable(dev)
            // 확인용은 맨 끝에 따로 둔다 — 규칙을 고치는 동안에는 이 절을 보지 않는다.
            appendLine()
            appendLine("## 사례별 (확인용 holdout)")
            caseTable(holdout)
        }
        println(report)
        File("build/reports/rule-regression").apply { mkdirs() }.resolve("report.md").writeText(report)

        val missedRealFailures = outcomes.filter { it.case.source == "real-failure" && !it.alerted }
        assertTrue(
            "실제로 놓쳤던 대화를 여전히 못 잡음: ${missedRealFailures.joinToString { "${it.case.id}(${it.score}점)" }}",
            missedRealFailures.isEmpty() || !ENFORCE
        )
        assertTrue("위험 대화 탐지율 ${pct(recall)} < 기준 ${pct(MIN_DANGER_RECALL)}", recall >= MIN_DANGER_RECALL || !ENFORCE)
        // 신호가 일부만 있는 대화가 단독으로 긴급이 되면 안 된다 (김선한 구현작업 v1: ambiguous 단독 CRITICAL 방지)
        val ambiguousCritical = outcomes.filter { it.case.kind == "ambiguous" && it.level == RiskLevel.CRITICAL }
        assertTrue("애매한 대화가 긴급으로 판정됨: ${ambiguousCritical.joinToString { "${it.case.id}(${it.score}점)" }}", ambiguousCritical.isEmpty() || !ENFORCE)
        assertTrue("일상 대화 오탐률 ${pct(falsePositive)} > 기준 ${pct(MAX_BENIGN_FALSE_POSITIVE)}", falsePositive <= MAX_BENIGN_FALSE_POSITIVE || !ENFORCE)
    }

    private fun StringBuilder.caseTable(list: List<Outcome>) {
        appendLine()
        appendLine("| id | 구분 | 판정 | 점수 | 결과 | 잡힌 표현 |")
        appendLine("|---|---|---|---|---|---|")
        list.forEach { o ->
            val verdict = when (o.case.kind) {
                "danger" -> if (o.alerted) "잡음" else "**놓침**"
                "benign" -> if (o.alerted) "**오탐**" else "정상"
                "ambiguous" -> if (o.level == RiskLevel.CRITICAL) "**애매→긴급**" else "-"
                else -> "-"
            }
            val evidence = (o.matched + o.directRules + o.combos).joinToString(", ").replace("|", "/")
            appendLine("| ${o.case.id} | ${o.case.kind} | ${o.level.label} | ${o.score} | $verdict | $evidence |")
        }
    }

    private fun pct(v: Double) = "%.0f%%".format(v * 100)

    private fun ratio(hit: Int, total: Int) = if (total == 0) "-" else "${pct(hit.toDouble() / total)} ($hit/$total)"

    private companion object {
        /**
         * 2026-09-15 행동 패턴 보강 후 측정값(개발용 위험 77% 탐지, 일상 0% 오탐) 기준으로 하한·상한을 둔다.
         * 규칙을 바꿔 이 선 아래로 떨어지면 테스트가 실패한다. 더 좋아지면 올린다(내리지 않는다).
         */
        const val ENFORCE = true
        const val MIN_DANGER_RECALL = 0.75
        const val MAX_BENIGN_FALSE_POSITIVE = 0.05
    }
}
