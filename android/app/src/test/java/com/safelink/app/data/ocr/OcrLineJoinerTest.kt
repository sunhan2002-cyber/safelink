package com.safelink.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrLineJoinerTest {

    @Test
    fun `말풍선 폭에서 글자 단위로 접힌 한국어 줄을 한 메시지로 잇는다`() {
        // 기기 테스트에서 스크린샷으로만 놓치던 실제 인식 결과
        assertEquals("한 시간 인에 300 안 보내면 전부 뿌린다", OcrLineJoiner.join(listOf("한 시간 인에 300 안 보내면 전부 뿌", "린다")))
        assertEquals("그냥 농담으로 하는 말이야, 왜 이렇게 심각해?", OcrLineJoiner.join(listOf("그냥 농담으로 하는 말이야, 왜 이렇게 심", "각해?")))
    }

    @Test
    fun `영어 단어 경계는 한 칸 띄운다`() {
        assertEquals("please confirm here", OcrLineJoiner.join(listOf("please confirm", "here")))
    }
}
