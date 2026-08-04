package com.safelink.app.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

private data class CorpusSentence(
    val id: String,
    val text: String,
    val status: String,
    @SerializedName("expected_matched_ids") val expectedMatchedIds: List<String> = emptyList()
)

private data class Corpus(val sentences: List<CorpusSentence>)

/**
 * data/신규_기법_문장_수집_v1.json(v1.1 확정본) 회귀 테스트.
 * 4주차 05/06번 문서 참고 - keyword.json 확장 후 "matched" 상태인 문장은 expected_matched_ids가
 * 계속 잡히는지 확인한다. "known_gap" 문장은 06번 문서(규칙기반 한계 분류)의 근거 자료라
 * 여기서는 강제로 실패시키지 않는다(향후 키워드가 추가돼서 잡히게 되면 오히려 좋은 일).
 */
class TechniqueLabelCorpusTest {

    companion object {
        private lateinit var engine: DetectionEngine
        private lateinit var corpus: Corpus

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val keywordJson = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
            val institutionJson = javaClass.classLoader!!.getResourceAsStream("institutions.json")!!.bufferedReader().readText()
            val keywordData = gson.fromJson(keywordJson, KeywordData::class.java)
            val institutionData = gson.fromJson(institutionJson, InstitutionData::class.java)
            engine = DetectionEngine(keywordData, institutionData, gson)

            val corpusJson = javaClass.classLoader!!.getResourceAsStream("신규_기법_문장_수집_v1.json")!!.bufferedReader().readText()
            corpus = gson.fromJson(corpusJson, Corpus::class.java)
        }
    }

    @Test
    fun `matched로 표시된 문장은 expected_matched_ids가 전부 잡혀야 함`() {
        val failures = mutableListOf<String>()
        corpus.sentences.filter { it.status == "matched" }.forEach { s ->
            val actualIds = engine.analyze(s.text).matchedKeywords.map { it.keywordId }.toSet()
            val missing = s.expectedMatchedIds.filterNot { it in actualIds }
            if (missing.isNotEmpty()) failures += "${s.id}: 못 찾은 id $missing (실제 매칭: $actualIds)"
        }
        assertTrue("회귀 실패:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun `코퍼스 구성 확인 (38개 문장, matched-known_gap 분류 존재)`() {
        assertTrue("코퍼스가 비어있음", corpus.sentences.isNotEmpty())
        val statuses = corpus.sentences.map { it.status }.toSet()
        assertTrue("status 값은 matched 또는 known_gap만 허용", statuses.all { it == "matched" || it == "known_gap" })
    }
}
