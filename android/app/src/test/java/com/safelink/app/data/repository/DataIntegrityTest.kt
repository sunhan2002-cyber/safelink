package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import com.safelink.app.data.model.raw.KeywordRiskTypeAddition
import com.safelink.app.data.model.raw.SubcategoryRiskTypeMapping
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * keyword.json <-> institutions.json 사이의 참조 무결성 + 키워드 80개 전수 매칭 커버리지 검사.
 * DetectionEngineTest(41개 케이스, 동작 검증)와는 관점이 다름 — 여기는 "데이터 자체가
 * 스스로 앞뒤가 맞는지"를 기계적으로 훑는다. 참고: 신기훈 4주차 결과물 03번 문서.
 */
class DataIntegrityTest {

    companion object {
        private lateinit var keywordData: KeywordData
        private lateinit var institutionData: InstitutionData
        private lateinit var engine: DetectionEngine
        private lateinit var subcategoryMappings: Map<String, SubcategoryRiskTypeMapping>
        private lateinit var keywordAdditions: Map<String, KeywordRiskTypeAddition>

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val keywordJson = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
            val institutionJson = javaClass.classLoader!!.getResourceAsStream("institutions.json")!!.bufferedReader().readText()
            keywordData = gson.fromJson(keywordJson, KeywordData::class.java)
            institutionData = gson.fromJson(institutionJson, InstitutionData::class.java)
            subcategoryMappings = institutionData.subcategoryMappings(gson)
            keywordAdditions = institutionData.keywordAdditions(gson)
            engine = DetectionEngine(keywordData, institutionData, gson)
        }
    }

    @Test
    fun `id 형식은 카테고리약어-중분류번호-일련번호 형식을 지켜야 함`() {
        val idPattern = Regex("^(VP|RS|GL)-\\d+-\\d+-\\d{3}$")
        val invalid = keywordData.keywords.map { it.id }.filterNot { idPattern.matches(it) }
        assertTrue("id 형식 위반: $invalid", invalid.isEmpty())
    }

    @Test
    fun `keyword json의 모든 subcategory_id는 institutions json의 subcategory_to_risk_type에 존재해야 함`() {
        val missing = keywordData.keywords.map { it.subcategoryId }.distinct()
            .filterNot { subcategoryMappings.containsKey(it) }
        assertTrue("subcategory_to_risk_type에 없는 subcategory_id: $missing", missing.isEmpty())
    }

    @Test
    fun `subcategory_to_risk_type의 risk_types는 risk_type_priority 키로 전부 존재해야 함`() {
        val allRiskTypes = subcategoryMappings.values.flatMap { it.riskTypes }.distinct()
        val missing = allRiskTypes.filterNot { institutionData.riskTypePriority.containsKey(it) }
        assertTrue("risk_type_priority에 없는 risk_type: $missing", missing.isEmpty())
    }

    @Test
    fun `risk_type_priority의 institution_id는 institutions 목록에 전부 존재해야 함`() {
        val institutionIds = institutionData.institutions.map { it.id }.toSet()
        val missing = institutionData.riskTypePriority.values.flatten()
            .map { it.institutionId }.distinct()
            .filterNot { it in institutionIds }
        assertTrue("institutions 목록에 없는 institution_id: $missing", missing.isEmpty())
    }

    @Test
    fun `keyword_risk_type_additions의 키는 keyword json에 실제 존재하는 id여야 함`() {
        val keywordIds = keywordData.keywords.map { it.id }.toSet()
        val missing = keywordAdditions.keys.filterNot { it in keywordIds }
        assertTrue("keyword.json에 없는데 keyword_risk_type_additions에 등록된 id: $missing", missing.isEmpty())
    }

    @Test
    fun `keyword_risk_type_additions의 add_risk_types도 risk_type_priority에 존재해야 함`() {
        val missing = keywordAdditions.values.flatMap { it.addRiskTypes }.distinct()
            .filterNot { institutionData.riskTypePriority.containsKey(it) }
        assertTrue("risk_type_priority에 없는 add_risk_types: $missing", missing.isEmpty())
    }

    @Test
    fun `weight는 0 이상이어야 함`() {
        val negative = keywordData.keywords.filter { it.weight < 0 }.map { it.id }
        assertTrue("weight가 음수인 항목: $negative", negative.isEmpty())
    }

    @Test
    fun `institutions 목록의 institution_id는 중복이 없어야 함`() {
        val ids = institutionData.institutions.map { it.id }
        val duplicated = ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        assertTrue("중복된 institution id: $duplicated", duplicated.isEmpty())
    }

    @Test
    fun `keyword json id는 전체가 유일해야 함`() {
        val ids = keywordData.keywords.map { it.id }
        val duplicated = ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        assertTrue("중복된 keyword id: $duplicated", duplicated.isEmpty())
    }

    /**
     * keyword.json 80개 항목 전부를 순회하면서, 각 항목의 keyword(문자열 그대로) 또는
     * regex-simple 3종의 대표 샘플을 엔진에 넣어 그 id가 실제로 matchedKeywords에
     * 잡히는지 확인한다. 41개 케이스 + 5개 샘플 검증에서는 커버되지 않았던, 나머지
     * 키워드들이 실제로 "죽은 키워드"가 아닌지 확인하는 전수 스윕.
     */
    @Test
    fun `keyword json 80개 항목 전수 매칭 커버리지`() {
        val regexSamples = mapOf(
            "VP-1-1-010" to "경찰서 직원입니다. 확인 부탁드립니다.",
            "VP-1-3-003" to "010-1234-5678",
            "VP-1-6-004" to "http://example.com",
            "RS-2-4-004" to "500만원 좀 빌려줘."
        )
        val failures = mutableListOf<String>()
        keywordData.keywords.forEach { entry ->
            val sample = if (entry.matchType == "keyword") entry.keyword else regexSamples[entry.id]
            if (sample == null) {
                failures += "${entry.id}: 검증용 샘플 문자열이 없음 (regexSamples 등록 누락 가능성)"
                return@forEach
            }
            val found = engine.analyze(sample).matchedKeywords.any { it.keywordId == entry.id }
            if (!found) failures += "${entry.id} ('$sample') - 매칭 안 됨"
        }
        assertTrue("매칭 실패 항목 ${failures.size}건:\n${failures.joinToString("\n")}", failures.isEmpty())
    }
}
