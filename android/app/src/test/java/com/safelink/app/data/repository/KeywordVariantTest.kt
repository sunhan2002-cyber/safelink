package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * 핵심 표현 24개 정규식 전환의 변형 대응 확인 (신기훈 탐지보강 변경정리 2-2, 김선한 구현작업 v1 6단계).
 * 각 id 가 어미·조사·띄어쓰기가 다른 변형을 잡는지, 평범한 문장에서는 새로 잡히지 않는지 본다.
 */
class KeywordVariantTest {

    companion object {
        private lateinit var engine: DetectionEngine

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val kw = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
            val inst = javaClass.classLoader!!.getResourceAsStream("institutions.json")!!.bufferedReader().readText()
            engine = DetectionEngine(gson.fromJson(kw, KeywordData::class.java), gson.fromJson(inst, InstitutionData::class.java), gson)
        }

        /** id → 새로 잡혀야 하는 변형 (신기훈 문서 '새로 잡히는 예' 열) */
        private val VARIANTS = mapOf(
        "FM-4-1-001" to listOf("엄마 나예요", "엄마인데 폰 바꿨어", "엄마나야"),
        "FM-4-1-002" to listOf("아빠인데 급해", "아빠 나예요"),
        "FM-4-1-003" to listOf("나 딸인데 폰이 고장", "나 딸이에요"),
        "FM-4-1-004" to listOf("나 아들인데", "나아들이야"),
        "FM-4-1-006" to listOf("언니인데 부탁 좀", "언니 나예요"),
        "FM-4-1-007" to listOf("폰 고장 났어", "휴대폰이 고장 나서", "핸드폰 고장이야"),
        "FM-4-1-011" to listOf("새 번호로 연락해", "새번호 저장해줘", "새 번호를 저장해"),
        "FM-4-2-001" to listOf("통화가 안 되니", "통화는 안 돼", "전화가 안 돼"),
        "FM-4-3-002" to listOf("대신송금해줄래", "대신 좀 송금해줘", "대신 돈 좀 보내줘"),
        "FM-4-4-003" to listOf("인증문자 좀 보내줘", "인증 문자 알려줘"),
        "RS-2-1-004" to listOf("프로필 보고 연락했어요", "프로필 사진 보고 메시지 드려요"),
        "RS-2-2-002" to listOf("진심으로 마음이 갔어요", "마음이 가네요"),
        "RS-2-2-003" to listOf("결혼하고 싶다", "당신이랑 결혼하구 싶어"),
        "RS-2-3-002" to listOf("아무한테도 말하지 마", "아무에게도 얘기하지 마세요", "아무한테도 알리지 말아줘"),
        "RS-2-4-007" to listOf("자리가 얼마 안 남았어", "자리 몇 개 안 남았어요"),
        "RS-2-5-002" to listOf("당신뿐이에요", "당신밖에 없어"),
        "RS-2-5-003" to listOf("다음 달에 꼭 갚을게", "며칠 뒤에 돌려줄게", "다음주에 갚겠습니다"),
        "RS-2-6-001" to listOf("텔레그램으로 옮겨서 얘기해요", "위챗으로 대화해요"),
        "RS-2-7-002" to listOf("영상통화는 안 돼요", "영상 통화가 힘들어요", "영상통화 못 해"),
        "RS-2-9-002" to listOf("부탁 하나만 해도 돼?", "부탁 좀 들어줄 수 있어요?"),
        "RS-2-10-002" to listOf("지금 포기하면 원금도 날아가요", "포기하면 돈 못 찾아요"),
        "VP-1-3-001" to listOf("담당자 연락처 알려드릴게요", "담당자 번호를 남겨드릴게요"),
        "VP-1-3-002" to listOf("이 번호로 연락 주세요", "아래 번호로 전화하세요", "해당 번호로 연락바랍니다"),
        "VP-1-4-012" to listOf("다른 사람한테 말하지 마세요", "주변에 상의하지 마십시오"),
        )

        /** 위 24개 표현과 단어가 겹치지만 위험하지 않은 평범한 문장 */
        private val PLAIN = listOf(
        "오늘 점심 뭐 먹을까",
        "엄마랑 마트 다녀왔어",
        "아빠 생신 선물 뭐가 좋을까",
        "딸이랑 놀이공원 갔어",
        "언니 결혼식이 다음 달이야",
        "휴대폰 요금제 바꿨어",
        "새 폰 사진이 잘 나오네",
        "통화 품질이 좋아졌어",
        "송금 수수료가 무료래",
        "인증서 갱신했어",
        "프로필 사진 바꿨네 예쁘다",
        "마음이 편해졌어",
        "결혼식 사회 부탁받았어",
        "비밀번호 까먹어서 재설정했어",
        "자리 예약해뒀어",
        "당신 덕분에 힘이 났어",
        "다음 주에 만나자",
        "텔레그램 앱 업데이트했어",
        "영상 편집 배우는 중이야",
        "부탁한 책 가져왔어",
        "포기하지 말고 끝까지 해보자",
        "담당자님께 메일 보냈어요",
        "번호표 뽑고 기다리는 중",
        "주변에 맛집 있어?",
        "다른 사람들도 다 왔어",
        "아무거나 괜찮아",
        "전화번호부 정리했어",
        )
    }

    private fun matchedIds(text: String) = engine.analyze(text).matchedKeywords.map { it.keywordId }.toSet()

    @Test
    fun `24개 전환 항목이 대표 변형을 모두 잡는다`() {
        val misses = VARIANTS.flatMap { (id, variants) -> variants.filterNot { id in matchedIds(it) }.map { "$id ← '$it'" } }
        assertTrue("못 잡은 변형 ${misses.size}건:\n" + misses.joinToString("\n"), misses.isEmpty())
    }

    @Test
    fun `변형은 24개 항목 전체에서 50개 이상이다`() {
        assertTrue(VARIANTS.size == 24 && VARIANTS.values.sumOf { it.size } >= 50)
    }

    @Test
    fun `평범한 문장에서는 24개 전환 항목이 잡히지 않는다`() {
        val hits = PLAIN.flatMap { text -> (matchedIds(text) intersect VARIANTS.keys).map { "$it ← '$text'" } }
        assertTrue("평범한 문장 오탐 ${hits.size}건:\n" + hits.joinToString("\n"), hits.isEmpty())
    }
}
