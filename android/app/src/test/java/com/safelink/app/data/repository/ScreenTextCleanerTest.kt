package com.safelink.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTextCleanerTest {

    @Test
    fun `문자 앱 대화 화면에서 메시지와 번호만 남는다`() {
        // 에뮬레이터 백그라운드 기록에 실제로 저장돼 있던 화면 텍스트
        val screen = """
            자기야 나 지금 해외 파병 중인데 통관비가 급하게 필요해 300만원만 보내줄 수 있어 돌아가면 꼭 갚을게
            Texting with 01044442222 (SMS/MMS)
            Show attach content screen
            Text message
            Show attach emoji and stickers screen
            Show attach media screen
            Show record a voice message screen
            01044442222
        """.trimIndent()
        assertEquals(
            "자기야 나 지금 해외 파병 중인데 통관비가 급하게 필요해 300만원만 보내줄 수 있어 돌아가면 꼭 갚을게\n01044442222",
            ScreenTextCleaner.clean(screen)
        )
    }

    @Test
    fun `대화 목록 화면의 요일·읽음 숫자·아이콘 라벨을 뺀다`() {
        val screen = "저 검찰청 수사관입니다. 즉시 안전계좌로 이체가 필요합니다.\nWed\nConversation Icon\n01055554444\n안녕하세요 회의 자료 보내드립니다\nWed\n1\nStart chat"
        assertEquals(
            "저 검찰청 수사관입니다. 즉시 안전계좌로 이체가 필요합니다.\n01055554444\n안녕하세요 회의 자료 보내드립니다",
            ScreenTextCleaner.clean(screen)
        )
    }

    @Test
    fun `시간 표시는 줄 전체일 때만 화면 요소로 본다`() {
        listOf("오후 3:12", "10:05", "3:12 PM", "2026년 9월 15일 월요일", "9월 15일", "어제", "5분 전", "99+", "메시지 입력")
            .forEach { assertTrue(it, ScreenTextCleaner.isScreenElement(it)) }
        listOf("오후 3시까지 입금하세요", "사진 보내줘", "300", "좋아요 눌러주면 5천원 드려요", "01012345678", "내일 10:00까지 보내")
            .forEach { assertFalse(it, ScreenTextCleaner.isScreenElement(it)) }
    }

    @Test
    fun `스크린샷 글자에 섞인 상태표시줄과 시각 줄을 뺀다`() {
        // 시연(2026-09-15) 스크린샷 분석 화면에 그대로 보이던 인식 결과
        val ocr = "SKT 10:24\n← 아들\n5월 20일 (화)\n엄마 지금 바로 확인해야 해\n링크 보냈어 바로 눌러줘\nll 100%을\n오전 10:20\n지금 안 하면 큰일 나\n오전 10:19"
        assertEquals("← 아들\n엄마 지금 바로 확인해야 해\n링크 보냈어 바로 눌러줘\n지금 안 하면 큰일 나", ScreenTextCleaner.clean(ocr))
        listOf("수익 30%", "원금 100% 보장", "LTE 요금제 바꿨어").forEach { assertFalse(it, ScreenTextCleaner.isScreenElement(it)) }
    }

    @Test
    fun `디스코드 서버 목록·보낸 사람 시각·입력창 안내를 뺀다`() {
        // 실기기(갤럭시 S23 FE) 디스코드 백그라운드 기록에 남아 있던 줄
        val screen = listOf(
            "읽지 않은 메시지, 캬루 갤러리 m", "즐겜하자", "1명 온라인", "sunhan04242",
            "이게 다 너 손해볼까봐 하는 말이야", "선한, 오후 7:04", "미디어 키보드 전환", "#일반에 메시지 보내기", "이모지 키보드 전환"
        ).joinToString("\n")
        assertEquals("즐겜하자\n이게 다 너 손해볼까봐 하는 말이야", ScreenTextCleaner.clean(screen))
    }

    @Test
    fun `디스코드 채널 목록·환영 문구·보낸 사람 아이디를 뺀다`() {
        // 실기기 디스코드 새 채널 기록(2026-09-15 23:45)
        val screen = listOf(
            "노트-자원", "채팅 채널", "일반 (채팅 채널)", "일반", "읽지 않은 숙제방 (채팅 채널)", "숙제방", "음성 채널",
            "공경진방 (음성 채널)", "공경진방", "#노트-자원에 오신 걸 환영합니다!", "#노트-자원 채널의 시작이에요.",
            "재겸", "오늘 저녁 뭐 먹을래?", "jaegyeom0247", "엄마", "jaegyeom0247", "나 폰 고장나서",
            "오후 6:57 이후로 읽지 않은 메시지가 11개 있어요", "hello"
        ).joinToString("\n")
        assertEquals("노트-자원\n재겸\n오늘 저녁 뭐 먹을래?\n엄마\n나 폰 고장나서\nhello", ScreenTextCleaner.clean(screen))
    }

    @Test
    fun `인스타그램 DM 스토리·활동 상태·사진 안내·입력 도움말을 뺀다`() {
        // 실기기 인스타 DM 백그라운드 기록(2026-09-16 00:00)
        val screen = listOf(
            "myantisthis님 스토리 열기", "🐜", "최근 활동: 16분 전", "jae_k03님이 사진 3/3장을 보냈습니다",
            "자기야 나 지금 두바이 공항인데", "어제 오전 12:00", "세관에서 짐이 걸렸어",
            "사라지는 메시지를 설정하려면 위로 살짝 미세요", "/silent 🤫 사용해보기", "음성 메시지, 녹음하려면 길게 누르세요"
        ).joinToString("\n")
        assertEquals("자기야 나 지금 두바이 공항인데\n세관에서 짐이 걸렸어", ScreenTextCleaner.clean(screen))
        assertFalse("물음표만 보내도 판정엔 상관없지만 글자 있는 메시지는 남긴다", ScreenTextCleaner.isScreenElement("ㅋㅋ 진짜?"))
    }

    @Test
    fun `같은 창을 두 번 훑어 생긴 바로 앞 줄 중복은 한 번만 남긴다`() {
        assertEquals("엄마 나 폰 고장났어\n대신 송금해줘\n엄마 나 폰 고장났어",
            ScreenTextCleaner.clean("엄마 나 폰 고장났어\n엄마 나 폰 고장났어\n대신 송금해줘\n엄마 나 폰 고장났어"))
    }

    @Test
    fun `위험 메시지 줄만 고른다 — 걸린 표현 또는 링크가 있는 줄`() {
        val text = "안녕하세요\n안전계좌로 이체하세요\n회의 자료입니다\n확인: bit.ly/abc12\n감사합니다"
        val s = text.indexOf("안전계좌")
        val picked = RiskyLines.select(text, listOf(s to s + 4)).map { text.substring(it.start, it.end) }
        assertEquals(listOf("안전계좌로 이체하세요", "확인: bit.ly/abc12"), picked)
        assertEquals(5, RiskyLines.lines(text).size)
    }
}
