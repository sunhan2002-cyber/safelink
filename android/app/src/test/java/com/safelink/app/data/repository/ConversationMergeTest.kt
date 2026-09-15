package com.safelink.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMergeTest {

    // 실기기 인스타 DM 기록(2026-09-16 00:00:02 → 00:00:11)
    private val first = "자기야 나 지금 두바이 공항인데\n세관에서 짐이 걸렸어"
    private val second = "자기야 나 지금 두바이 공항인데\n세관에서 짐이 걸렸어\n수수료만 내면 바로 풀린대"
    private val third = "자기야 나 지금 두바이 공항인데\n세관에서 짐이 걸렸어\n수수료만 내면 바로 풀린대\n한국 가서 바로 돌려줄게"

    @Test
    fun `메시지가 이어서 올라온 같은 대화는 하나로 본다`() {
        assertTrue(ConversationMerge.isSameConversation(first, second))
        assertTrue(ConversationMerge.isSameConversation(second, third))
    }

    @Test
    fun `스크롤로 위쪽 줄이 빠져도 같은 대화로 본다`() {
        val scrolled = "수수료만 내면 바로 풀린대\n한국 가서 바로 돌려줄게\n나 믿지? 우리 곧 만나잖아"
        assertTrue(ConversationMerge.isSameConversation(third, scrolled))
    }

    @Test
    fun `이름·방 이름 같은 짧은 줄만 겹치는 다른 대화는 따로 둔다`() {
        val familyScam = "노트-자원\n재겸\n엄마 나 폰 고장나서\n친구폰으로 연락해\n급하게 문화상품권 사서 번호 보내줘"
        val romance = "노트-자원\n재겸\n자기야 나 지금 두바이 공항인데\n세관에서 짐이 걸렸어"
        assertFalse(ConversationMerge.isSameConversation(familyScam, romance))
    }
}
