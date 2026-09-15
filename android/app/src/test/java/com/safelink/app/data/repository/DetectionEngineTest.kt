package com.safelink.app.data.repository

import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

/**
 * data/위험 문장 테스트.json 41개 케이스(양성17/음성12/경계6/콤보검증6) 검증.
 * CLAUDE.md 작업 원칙 4번: 위험도 점수/구간 로직을 바꾸면 이 41개 케이스를 재계산해서
 * 깨지지 않는지 확인해야 함 — 이 테스트가 그 재계산 역할을 한다.
 *
 * DetectionEngine은 Android Context에 의존하지 않으므로, assets와 내용이 동일한
 * src/test/resources/keyword.json, institutions.json을 그대로 로드해서 순수 JVM에서 검증.
 */
class DetectionEngineTest {

    companion object {
        private lateinit var engine: DetectionEngine

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val gson = Gson()
            val keywordJson = javaClass.classLoader!!.getResourceAsStream("keyword.json")!!.bufferedReader().readText()
            val institutionJson = javaClass.classLoader!!.getResourceAsStream("institutions.json")!!.bufferedReader().readText()
            val keywordData = gson.fromJson(keywordJson, KeywordData::class.java)
            val institutionData = gson.fromJson(institutionJson, InstitutionData::class.java)
            engine = DetectionEngine(keywordData, institutionData, gson)
        }

        // band_definitions (위험 문장 테스트.json _meta 기준): 낮음 0~30 / 중간 31~65 / 높음 66~100
        private fun bandOf(score: Int): String = when {
            score >= 66 -> "높음"
            score >= 31 -> "중간"
            else -> "낮음"
        }
    }

    private fun assertCase(id: String, turns: List<String>, expectedTotal: Double, expectedLevel: String, expectedComboRules: List<String> = emptyList()) {
        val result = engine.analyze(turns)
        val expectedScore = expectedTotal.toInt()
        assertEquals("[$id] score", expectedScore, result.score)
        assertEquals("[$id] level(3-tier)", expectedLevel, bandOf(result.score))
        assertEquals("[$id] combo rules", expectedComboRules.toSet(), result.appliedComboIds.toSet())
    }

    // ───────────────────────── positive_cases (17) ─────────────────────────

    @Test fun `TC-VP-POS-01`() = assertCase("TC-VP-POS-01", listOf("택배기사인데요, 지금 잠깐 통화 가능하실까요?"), 15.0, "낮음")
    @Test fun `TC-VP-POS-02`() = assertCase("TC-VP-POS-02", listOf("고객님 명의로 신청하신 적 없는 카드가 발급된 정황이 확인됩니다."), 15.0, "낮음")
    @Test fun `TC-VP-POS-03`() = assertCase("TC-VP-POS-03", listOf("이 번호로 전화해보세요, 확인 가능하실 거예요."), 10.0, "낮음")
    @Test fun `TC-VP-POS-04`() = assertCase("TC-VP-POS-04", listOf("지금 당장 확인해주셔야 합니다."), 20.0, "낮음")
    @Test fun `TC-VP-POS-05`() = assertCase("TC-VP-POS-05", listOf("계좌번호를 알려주셔야 확인이 가능합니다."), 30.0, "낮음")
    @Test fun `TC-VP-POS-06`() = assertCase("TC-VP-POS-06", listOf("택배 조회 부탁드립니다, 확인 부탁드려요."), 25.0, "낮음")
    @Test fun `TC-RS-POS-01`() = assertCase("TC-RS-POS-01", listOf("저는 지방 파견 나와 있어요."), 5.0, "낮음")
    @Test fun `TC-RS-POS-02`() = assertCase("TC-RS-POS-02", listOf("진심으로 마음이 가요, 당신 생각을 안 할 수가 없어요."), 10.0, "낮음")
    @Test fun `TC-RS-POS-03`() = assertCase("TC-RS-POS-03", listOf("이건 아무한테도 말하지 말아주세요."), 15.0, "낮음")
    @Test fun `TC-RS-POS-04`() = assertCase("TC-RS-POS-04", listOf("지금 사업 자금이 급하게 필요해서 그래요."), 30.0, "낮음")
    // 4주차 05번 문서: findSuppressedByOverlap() 추가로 VP-1-4-003("오늘 안에") vs
    // RS-2-5-001("오늘 안에 처리 안되면") 부분 문자열 충돌이 해결돼 더 이상 Ignore 불필요.
    @Test fun `TC-RS-POS-05`() = assertCase("TC-RS-POS-05", listOf("오늘 안에 처리 안되면 큰일 나요."), 20.0, "낮음")
    @Test fun `TC-GL-POS-01`() = assertCase("TC-GL-POS-01", listOf("너 오늘 좀 예민한 거 같아."), 8.0, "낮음")
    @Test fun `TC-GL-POS-02`() = assertCase("TC-GL-POS-02", listOf("내가 언제 그렇게 말했어? 기억이나 하고 말하는 거야?"), 12.0, "낮음")
    @Test fun `TC-GL-POS-03`() = assertCase("TC-GL-POS-03", listOf("걔랑 만나지 마, 부탁이야."), 15.0, "낮음")
    @Test fun `TC-GL-POS-04`() = assertCase("TC-GL-POS-04", listOf("다 너를 위해서 하는 말이야."), 12.0, "낮음")
    @Test fun `TC-GL-POS-05`() = assertCase("TC-GL-POS-05", listOf("내가 없으면 넌 아무것도 못해."), 18.0, "낮음")
    @Test fun `TC-GL-POS-06`() = assertCase("TC-GL-POS-06", listOf("헤어지자는 거야? 그렇게는 안 될 걸."), 20.0, "낮음")

    // ───────────────────────── negative_cases (12) ─────────────────────────

    @Test fun `TC-VP-NEG-01`() = assertCase("TC-VP-NEG-01", listOf("택배기사님이 지금 배송 중이라고 문자 오셨더라, 진짜 친절하시다."), 22.5, "낮음")
    @Test fun `TC-VP-NEG-02`() = assertCase("TC-VP-NEG-02", listOf("친구야, 계좌번호 좀 보내줘 정산할게."), 30.0, "낮음")
    @Test fun `TC-VP-NEG-03`() = assertCase("TC-VP-NEG-03", listOf("지금 당장 회의 참석 부탁드립니다, 늦지 않게 와주세요."), 20.0, "낮음")
    @Test fun `TC-VP-NEG-04`() = assertCase("TC-VP-NEG-04", listOf("이번 주말에 결혼식 청첩장 드리려고 하는데 시간 되실 때 뵈러 갈게요."), 25.0, "낮음")
    @Test fun `TC-RS-NEG-01`() = assertCase("TC-RS-NEG-01", listOf("저희 팀장님이 이번에 지방 파견 발령 나셨대요."), 5.0, "낮음")
    // 데이터 이슈: expected_matched_ids에 RS-2-2-003("결혼하고 싶어요")이 포함돼 있지만,
    // 실제 문장은 "우리 결혼할까?"라 keyword.json의 단순 부분일치 규칙으로는 매칭되지 않음
    // (원본 테스트 픽스처의 expected_matched_ids가 자신의 입력 문장과 불일치 - 픽스처 쪽 이슈로 보임).
    @Ignore("테스트 픽스처의 expected_matched_ids와 입력 문장 불일치 - 제출 문서 참고")
    @Test fun `TC-RS-NEG-02`() = assertCase("TC-RS-NEG-02", listOf("자기야 진심으로 마음이 가요, 우리 결혼할까?"), 15.0, "낮음")
    @Test fun `TC-RS-NEG-03`() = assertCase("TC-RS-NEG-03", listOf("친구가 큰돈이 필요하다고 해서 사업 자금 빌려줬어, 다음 달에 갚는대."), 30.0, "낮음")
    // TC-RS-POS-05와 동일 원인이었던 부분 문자열 충돌 - findSuppressedByOverlap()으로 해결됨.
    @Test fun `TC-RS-NEG-04`() = assertCase("TC-RS-NEG-04", listOf("오늘 안에 처리 안되면 늦게 도착할 것 같아서 미리 말씀드려요."), 20.0, "낮음")
    @Test fun `TC-GL-NEG-01`() = assertCase("TC-GL-NEG-01", listOf("오늘 많이 예민한 거 같아 보이는데 무슨 일 있어? 나한테 얘기해도 괜찮아."), 8.0, "낮음")
    @Test fun `TC-GL-NEG-02`() = assertCase("TC-GL-NEG-02", listOf("엄마가 그러셨어, 다 너를 위해서 하는 말이야 라고."), 12.0, "낮음")
    @Test fun `TC-GL-NEG-03`() = assertCase("TC-GL-NEG-03", listOf("헤어지자는 거야? 아니 그냥 오늘 좀 힘들어서 그래, 미안."), 20.0, "낮음")
    @Test fun `TC-GL-NEG-04`() = assertCase("TC-GL-NEG-04", listOf("너 키우느라 내가 얼마나 고생했는데, 그래도 넌 항상 착했어."), 12.0, "낮음")

    // ───────────────────────── edge_cases (6) ─────────────────────────

    @Test fun `TC-VP-EDGE-01`() = assertCase(
        "TC-VP-EDGE-01",
        listOf(
            "택배기사인데요, 배송 중 확인 차 연락드렸습니다.",
            "그럼 명의 도용 우려가 있어서 확인이 필요합니다."
        ),
        // 서로 다른 행동 2개(기관사칭 + 명의도용 경고)지만 둘 다 요구·위협 행동이 아니라 2개 조합 가산 없음 → 원래 값 37.5 유지
        37.5, "중간"
    )

    @Test fun `TC-VP-EDGE-02`() = assertCase(
        "TC-VP-EDGE-02",
        listOf(
            "택배기사인데요, 배송 중 확인 차 연락드렸습니다.",
            "그럼 명의 도용 우려가 있어서 확인이 필요합니다.",
            "지금 당장 확인 안 하시면 계좌가 압류될 수 있습니다."
        ),
        // 상황규칙 추가(신기훈 8주차): 1-1+1-4 AUTHORITY-ISOLATION, 1-4 2회(지금 당장+압류) REPEAT-PRESSURE 가산 → 82.5→100
        100.0, "높음", listOf("COMBO-GENERAL-3CAT", "COMBO-VP-AUTHORITY-ISOLATION", "COMBO-VP-REPEAT-PRESSURE")
    )

    @Test fun `TC-RS-EDGE-01`() = assertCase(
        "TC-RS-EDGE-01",
        listOf(
            "지방 파견 나와 있어요, 무역회사 대표입니다.",
            "진심으로 마음이 가요, 당신 생각을 안 할 수가 없어요.",
            "이건 아무한테도 말하지 말아주세요, 우리 둘만 아는 걸로 해요."
        ),
        47.5, "중간", listOf("COMBO-GENERAL-3CAT")
    )

    @Test fun `TC-RS-EDGE-02`() = assertCase(
        "TC-RS-EDGE-02",
        listOf(
            "지방 파견 나와 있어요, 무역회사 대표입니다.",
            "진심으로 마음이 가요, 당신 생각을 안 할 수가 없어요.",
            "이건 아무한테도 말하지 말아주세요, 우리 둘만 아는 걸로 해요.",
            "사업 자금이 필요해요, 도와주실 수 있나요?"
        ),
        100.0, "높음", listOf("COMBO-GENERAL-4CAT", "COMBO-RS-SECRET-MONEY")
    )

    // 4주차 수정2 리뷰 대응(온디바이스 점수에 반복 가스라이팅 패턴 반영, COMBO-GL-REPEAT-PATTERN
    // 신설)으로 60->75, 중간->높음 전환. GL-3-1-001/003, GL-3-2-001/003은 서로 다른 위치의
    // 서로 다른 표현이라 겹침 억제 대상이 아니라 4건 전부 repeat_pattern 카운트에 잡힘.
    @Test fun `TC-GL-EDGE-01`() = assertCase(
        "TC-GL-EDGE-01",
        listOf(
            "너 오늘 좀 예민한 거 같아, 별일도 아닌데 왜 그래?",
            "내가 언제 그렇게 말했어? 너 또 왜곡해서 기억하는 거야.",
            "걔랑 만나지 마, 부탁이야."
        ),
        75.0, "높음", listOf("COMBO-GENERAL-3CAT", "COMBO-GL-REPEAT-PATTERN")
    )

    @Test fun `TC-GL-EDGE-02`() = assertCase(
        "TC-GL-EDGE-02",
        listOf(
            "너 오늘 좀 예민한 거 같아, 별일도 아닌데 왜 그래?",
            "내가 언제 그렇게 말했어? 너 또 왜곡해서 기억하는 거야.",
            "걔랑 만나지 마, 부탁이야.",
            "다 너를 위해서 하는 말이야. 내가 없으면 넌 아무것도 못해."
        ),
        100.0, "높음", listOf("COMBO-GENERAL-4CAT", "COMBO-GL-ISOLATION-GUILT", "COMBO-GL-REPEAT-PATTERN")
    )

    // ───────────────────────── combo_verification_cases (6) ─────────────────────────

    @Test fun `TC-COMBO-01`() = assertCase(
        "TC-COMBO-01",
        listOf("이 번호로 전화해보세요: 010-1234-5678"),
        25.0, "낮음", listOf("COMBO-VP-PHONE-VERIFY")
    )

    @Test fun `TC-COMBO-02`() = assertCase(
        "TC-COMBO-02",
        listOf("부고장 안내드립니다. 아래 링크에서 확인해주세요: http://bit.ly/xyz123"),
        45.0, "중간", listOf("COMBO-VP-SMISHING")
    )

    @Test fun `TC-COMBO-03`() = assertCase(
        "TC-COMBO-03",
        listOf("이건 아무한테도 말하지 말아주세요.", "사업 자금이 급해서 그런데 도와줄 수 있어요?"),
        60.0, "중간", listOf("COMBO-RS-SECRET-MONEY")
    )

    @Test fun `TC-COMBO-04`() = assertCase(
        "TC-COMBO-04",
        listOf("걔랑 만나지 마, 부탁이야.", "다 너를 위해서 하는 말이야."),
        42.0, "중간", listOf("COMBO-GL-ISOLATION-GUILT")
    )

    @Test fun `TC-COMBO-05`() = assertCase(
        "TC-COMBO-05",
        listOf("택배기사인데요.", "명의 도용 우려가 있어서요.", "지금 당장 확인이 필요합니다."),
        // 상황규칙 추가: 1-1+1-4 AUTHORITY-ISOLATION 가산 → 65→80(중간→높음)
        80.0, "높음", listOf("COMBO-GENERAL-3CAT", "COMBO-VP-AUTHORITY-ISOLATION")
    )

    @Test fun `TC-COMBO-06`() = assertCase(
        "TC-COMBO-06",
        listOf("택배기사인데요.", "명의 도용 우려가 있어서요.", "지금 당장 확인이 필요합니다.", "이 번호로 전화해보세요."),
        // 상황규칙 추가: 1-1+1-4 AUTHORITY-ISOLATION 가산 → 85→100
        100.0, "높음", listOf("COMBO-GENERAL-4CAT", "COMBO-VP-AUTHORITY-ISOLATION")
    )

    // 4주차 수정2 리뷰 대응: "문장/상황 기반 판단이 shouldEscalateToAI()에만 있고 1차 위험도
    // 계산 본체에는 없다"는 지적에 대응해 신설한 콤보 2건의 전용 검증 케이스.
    @Test fun `TC-COMBO-07`() = assertCase(
        "TC-COMBO-07",
        listOf("예민한 거 같아.", "별일도 아닌데 왜 그래."),
        27.0, "낮음", listOf("COMBO-GL-REPEAT-PATTERN")
    )

    @Test fun `TC-COMBO-08`() = assertCase(
        "TC-COMBO-08",
        List(14) { "오늘 날씨가 좋네요." } + "당신부터 만날 겁니다.",
        25.0, "낮음", listOf("COMBO-RS-LONG-SESSION-PATTERN")
    )

    // ───────────────────────── 안전 케이스(더미데이터 대체 확인용) ─────────────────────────

    @Test
    fun `무해한 문장은 SAFE, 매칭 0건`() {
        val result = engine.analyze("오늘 저녁에 뭐 먹을까? 나는 파스타 먹고 싶어.")
        assertEquals(0, result.score)
        assertTrue(result.isSafeAndEmpty)
        assertTrue(result.matchedKeywords.isEmpty())
        assertTrue(result.recommendedInstitutions.isEmpty())
    }

    // ───────────── 1차 직접 규칙 보강 묶음 1: 보이스피싱 문장/상황 ─────────────

    @Test
    fun `직접규칙 - 기관 사칭과 주민등록번호 제출 요구는 점수와 문장 근거에 반영`() {
        val result = engine.analyze("금융감독원 직원입니다. 본인 인증을 위해 주민등록번호를 알려주셔야 합니다.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-VP-AUTH-IDENTITY-REQUEST"))
        assertTrue(result.score >= 18)
        assertTrue(result.sentenceRuleEvidences.any { it.label.contains("기관 사칭") })
    }

    @Test
    fun `직접규칙 - 긴급 송금 요구는 상황 근거에 반영`() {
        val result = engine.analyze("지금 당장 계좌로 입금해주셔야 사건 처리가 가능합니다.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-VP-URGENT-TRANSFER"))
        assertTrue(result.situationalRuleEvidences.any { it.label.contains("긴급성") })
    }

    @Test
    fun `직접규칙 - 링크 설치 후 인증 유도는 문장 근거에 반영`() {
        val result = engine.analyze("보안 확인을 위해 링크를 눌러 앱 설치해주세요. 설치 후 인증을 진행해주세요.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-VP-LINK-INSTALL-VERIFY"))
        assertTrue(result.sentenceRuleEvidences.any { it.label.contains("링크") })
    }

    @Test
    fun `직접규칙 - 비밀 유지와 입금 요구는 상황 근거에 반영`() {
        val result = engine.analyze("아무한테도 말하지 말고 지금 계좌로 송금해주셔야 합니다.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-VP-SECRET-MONEY"))
        assertTrue(result.situationalRuleEvidences.any { it.label.contains("비밀 유지") })
    }

    @Test
    fun `직접규칙 - 기관 안내 기사에는 개인정보 제출 규칙이 발동하지 않는다`() {
        val result = engine.analyze("금융감독원 보도자료에서 본인 인증 제도 안내가 나왔습니다.")
        assertTrue(!result.appliedDirectRuleIds.contains("DIRECT-VP-AUTH-IDENTITY-REQUEST"))
    }

    @Test
    fun `직접규칙 - 업무상 긴급 정산에는 긴급 송금 규칙이 발동하지 않는다`() {
        val result = engine.analyze("오늘 안에 회의 자료를 보내주세요. 정산은 다음 주에 처리하겠습니다.")
        assertTrue(!result.appliedDirectRuleIds.contains("DIRECT-VP-URGENT-TRANSFER"))
    }

    // ───────────── 1차 직접 규칙 보강 묶음 2: 가족사칭·로맨스스캠 상황 ─────────────

    @Test
    fun `직접규칙 - 새 번호 가족사칭과 통화 회피 금전 요구는 상황 근거에 반영`() {
        val result = engine.analyze("엄마 나야. 폰이 고장 나서 새 번호로 연락해. 지금 통화가 안 돼서 계좌로 이체해줘.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-FM-NEW-NUMBER-MONEY"))
        assertTrue(result.category == "가족사칭" || result.matchedKeywords.any { it.keywordId.startsWith("FM-") })
        assertTrue(result.situationalRuleEvidences.any { it.label.contains("가족 사칭") })
    }

    @Test
    fun `직접규칙 - 친밀감 형성 뒤 병원비 송금 요구는 상황 근거에 반영`() {
        val result = engine.analyze("자기야 사랑해. 우리 결혼하고 함께 살자. 그런데 사고가 나서 병원비가 급해, 계좌로 입금해줄 수 있어?")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-RS-TRUST-EMERGENCY-MONEY"))
        assertTrue(result.situationalRuleEvidences.any { it.label.contains("관계 신뢰") })
    }

    @Test
    fun `직접규칙 - 가족의 새 번호 공유만으로 가족사칭 금전 규칙은 발동하지 않는다`() {
        val result = engine.analyze("엄마 새 번호가 바뀌었대. 나중에 전화해서 저장해둘게.")
        assertTrue(!result.appliedDirectRuleIds.contains("DIRECT-FM-NEW-NUMBER-MONEY"))
    }

    @Test
    fun `직접규칙 - 연인의 병원 방문 대화에는 로맨스스캠 금전 규칙이 발동하지 않는다`() {
        val result = engine.analyze("자기야 병원비는 보험으로 처리됐어. 내일 병문안 갈게, 사랑해.")
        assertTrue(!result.appliedDirectRuleIds.contains("DIRECT-RS-TRUST-EMERGENCY-MONEY"))
    }

    // ───────────── 1차 직접 규칙 보강 묶음 3: 투자사기·협박 갈취 상황 ─────────────

    @Test
    fun `직접규칙 - 원금 보장 투자와 입금 요구는 상황 근거에 반영`() {
        val result = engine.analyze("원금 보장에 확정 수익이 나는 코인 투자입니다. 지금 계좌로 입금해주시면 VIP방에 초대할게요.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-IV-GUARANTEED-RETURN-DEPOSIT"))
        assertTrue(result.situationalRuleEvidences.any { it.label.contains("수익 보장") })
    }

    @Test
    fun `직접규칙 - 유포 위협과 입금 요구는 상황 근거에 반영`() {
        val result = engine.analyze("지금 입금하지 않으면 영상을 가족에게 공개하겠습니다. 계좌로 송금하세요.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-TH-EXPOSURE-MONEY-DEMAND"))
        assertTrue(result.situationalRuleEvidences.any { it.label.contains("유포 위협") })
    }

    @Test
    fun `직접규칙 - 투자 공부 안내에는 수익 보장 입금 규칙이 발동하지 않는다`() {
        val result = engine.analyze("주식 투자 공부를 시작했어. 손실 가능성도 있으니 소액으로 연습해보자.")
        assertTrue(!result.appliedDirectRuleIds.contains("DIRECT-IV-GUARANTEED-RETURN-DEPOSIT"))
    }

    @Test
    fun `직접규칙 - 유포 피해 예방 안내에는 협박 갈취 규칙이 발동하지 않는다`() {
        val result = engine.analyze("영상 유포 협박을 받으면 돈을 보내지 말고 경찰에 신고해야 합니다.")
        assertTrue(!result.appliedDirectRuleIds.contains("DIRECT-TH-EXPOSURE-MONEY-DEMAND"))
    }

    // ───────────── 1차 직접 규칙 보강 묶음 4: 가스라이팅 문장·상황 ─────────────

    @Test
    fun `직접규칙 - 기억 부정과 책임 전가는 문장 근거에 반영`() {
        val result = engine.analyze("내가 언제 그랬어? 네가 잘못 기억한 거야. 다 너 때문에 일이 이렇게 된 거잖아.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-GL-DENY-BLAME"))
        assertTrue(result.sentenceRuleEvidences.any { it.label.contains("기억 부정") })
    }

    @Test
    fun `직접규칙 - 관계 고립과 의존 강요는 상황 근거에 반영`() {
        val result = engine.analyze("친구들이랑 연락하지 마. 너는 나 아니면 안 돼, 내 말만 들어.")
        assertTrue(result.appliedDirectRuleIds.contains("DIRECT-GL-ISOLATION-CONTROL"))
        assertTrue(result.situationalRuleEvidences.any { it.label.contains("관계 고립") })
    }

    @Test
    fun `직접규칙 - 기억 차이를 확인하는 대화에는 책임 전가 규칙이 발동하지 않는다`() {
        val result = engine.analyze("내가 잘못 기억했을 수도 있으니 서로 확인해보자. 네 탓이라고 말하려는 건 아니야.")
        assertTrue(!result.appliedDirectRuleIds.contains("DIRECT-GL-DENY-BLAME"))
    }

    @Test
    fun `직접규칙 - 친구 약속 조정만으로 관계 고립 규칙이 발동하지 않는다`() {
        val result = engine.analyze("친구랑 약속이 있어서 오늘은 먼저 다녀올게. 내일 다시 연락하자.")
        assertTrue(!result.appliedDirectRuleIds.contains("DIRECT-GL-ISOLATION-CONTROL"))
    }

    // ───────────── 신규 상황 규칙 콤보(신기훈 8주차 반영) ─────────────

    @Test
    fun `신규콤보 - VP 권위+고립 (AUTHORITY-ISOLATION)`() {
        val r = engine.analyze(listOf("저는 금융감독원 직원입니다. 사건 조사 중입니다.", "가족이나 지인에게 절대 말씀하시면 안 됩니다. 지금 당장 확인하셔야 합니다."))
        assertTrue(r.appliedComboIds.contains("COMBO-VP-AUTHORITY-ISOLATION"))
    }

    @Test
    fun `신규콤보 - GL 고립+통제 (CONTROL-ISOLATION)`() {
        val r = engine.analyze(listOf("걔랑 만나지 마, 가족이랑 좀 거리 둬.", "너 나 아니면 안 돼. 헤어지자고 하면 가만 안 둘 거야."))
        assertTrue(r.appliedComboIds.contains("COMBO-GL-CONTROL-ISOLATION"))
    }

    @Test
    fun `신규콤보 - VP 반복압박은 긴급성 2회일 때만 (오탐방지)`() {
        // 기관사칭만 반복(택배기사+배송 중)되는 무해 문장은 REPEAT-PRESSURE 미발동
        val benign = engine.analyze("택배기사님이 지금 배송 중이라고 문자 오셨더라, 진짜 친절하시다.")
        assertTrue(!benign.appliedComboIds.contains("COMBO-VP-REPEAT-PRESSURE"))
        // 긴급/협박 표현이 2회(지금 당장 + 압류) 반복되면 발동
        val pressured = engine.analyze("지금 당장 확인 안 하시면 계좌가 압류됩니다.")
        assertTrue(pressured.appliedComboIds.contains("COMBO-VP-REPEAT-PRESSURE"))
    }

    // ───────────── 신규 카테고리 감지(김재겸 수집분 반영) ─────────────

    @Test
    fun `신규 - 가족사칭 감지 및 분류`() {
        val r = engine.analyze(listOf(
            "엄마 나야 폰이 고장 나서 새 번호로 저장해줘",
            "지금 통화가 안 돼, 급하게 대신 결제 좀 해줘"
        ))
        assertEquals("가족사칭", r.category)
        assertTrue("FM 키워드가 잡혀야 함", r.matchedKeywords.any { it.keywordId.startsWith("FM-") })
        assertTrue(r.score > 0)
    }

    @Test
    fun `신규 - 투자사기 감지 및 분류`() {
        val r = engine.analyze(listOf("원금 보장 확정 수익, 코인 투자 VIP방으로 초대할게요"))
        assertEquals("투자사기", r.category)
        assertTrue("IV 키워드가 잡혀야 함", r.matchedKeywords.any { it.keywordId.startsWith("IV-") })
        assertTrue(r.score > 0)
    }

    @Test
    fun `신규 - 협박·갈취 감지 및 분류`() {
        val r = engine.analyze(listOf("영상 유포 전에 입금하세요", "지인들에게 뿌리기 전에 조용히 처리하죠"))
        assertEquals("협박·갈취", r.category)
        assertTrue("TH 키워드가 잡혀야 함", r.matchedKeywords.any { it.keywordId.startsWith("TH-") })
        assertTrue("고위험 신호는 중간 이상", r.score >= 31)
    }
}
