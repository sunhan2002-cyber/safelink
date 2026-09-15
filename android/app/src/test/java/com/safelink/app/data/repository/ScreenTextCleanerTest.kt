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
