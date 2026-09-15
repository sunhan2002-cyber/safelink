package com.safelink.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safelink.app.data.repository.DetectionRepository
import com.safelink.app.data.repository.RecordRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 백그라운드 기록 이어서 갱신 — 실제 Room DB 로 확인한다. 테스트가 만든 기록은 끝에 지운다.
 * (실기기 인스타 DM 로맨스스캠 4줄이 3초 사이 기록 3건으로 쌓이던 상황을 재현)
 */
@RunWith(AndroidJUnit4::class)
class RecordMergeDeviceTest {

    @Test
    fun sameConversationUpdatesOneRecord() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val analyzer = DetectionRepository(context)
        val records = RecordRepository(context)
        val lines = listOf("자기야 나 지금 두바이 공항인데", "세관에서 짐이 걸렸어", "수수료만 내면 바로 풀린대", "한국 가서 바로 돌려줄게")
        val created = mutableSetOf<String>()
        try {
            val ids = (2..4).map { n ->
                records.saveBackgroundDetection(analyzer.analyze(lines.take(n).joinToString("\n"))).also { created += it }
            }
            assertEquals("같은 대화는 기록 하나", 1, ids.toSet().size)
            val saved = records.findById(ids.last())!!
            assertEquals("가장 최근 원문으로 갱신", lines.joinToString("\n"), saved.originalText)
            assertEquals(analyzer.analyze(lines.joinToString("\n")).score, saved.score)

            // 스크롤로 위험 문장이 빠져 점수가 낮아진 화면은 더 위험했던 기록을 그대로 둔다
            val lower = records.saveBackgroundDetection(analyzer.analyze("세관에서 짐이 걸렸어\n수수료만 내면 바로 풀린대")).also { created += it }
            assertEquals(ids.last(), lower)
            assertEquals(lines.joinToString("\n"), records.findById(lower)!!.originalText)

            // 다른 대화는 새 기록
            val other = records.saveBackgroundDetection(
                analyzer.analyze("엄마 나 폰 고장나서\n친구폰으로 연락해\n지금 통화는 안돼\n급하게 문화상품권 사서 번호 보내줘")
            ).also { created += it }
            assertNotEquals(ids.last(), other)
        } finally {
            created.forEach { records.delete(it) }
        }
    }
}
