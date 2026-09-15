package com.safelink.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.ocr.MlKitOcrService
import com.safelink.app.data.repository.ConversationTurns
import com.safelink.app.data.repository.DetectionRepository
import com.safelink.app.data.repository.ScreenTextCleaner
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 에뮬레이터·실기기에서 분석 경로를 그대로 돌려 본다. 기록 DB 에는 저장하지 않는다.
 *
 * - 텍스트: 입력한 대화 → [DetectionRepository.analyze] (대화 분석 화면과 같은 호출).
 *   JVM 회귀 테스트와 결과가 다르면 안드로이드 정규식 엔진 차이가 원인이다.
 * - 스크린샷: 대화를 메신저 말풍선 이미지로 그림 → 앱의 ML Kit 인식 → 화면 요소 정리 → 분석.
 *   말풍선 폭에서 줄이 접히고 인식이 틀리는 실제 스크린샷 조건을 흉내 낸다.
 *
 * 실행: ./gradlew :app:assembleDebugAndroidTest 후 am instrument (결과는 앱 filesDir 의 pipeline 폴더에 tsv 로 남는다)
 */
@RunWith(AndroidJUnit4::class)
class PipelineDeviceTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testAssets get() = InstrumentationRegistry.getInstrumentation().context.assets

    private data class Case(val id: String, val kind: String, val source: String, val text: String)

    private fun cases(): List<Case> {
        val json = testAssets.open("rule_regression_cases.json").bufferedReader().readText()
        return JsonParser.parseString(json).asJsonObject.getAsJsonArray("cases").map {
            val o = it.asJsonObject
            Case(o["id"].asString, o["kind"].asString, o["source"].asString, o["text"].asString)
        }
    }

    private fun outFile(name: String) = File(context.filesDir, "pipeline").apply { mkdirs() }.resolve(name)

    @Test
    fun textPath() {
        val repo = DetectionRepository(context)
        val out = StringBuilder("id\tkind\tsource\tlevel\tscore\n")
        cases().forEach { c ->
            val r = repo.analyze(c.text)
            out.append("${c.id}\t${c.kind}\t${c.source}\t${r.riskLevel}\t${r.score}\n")
        }
        outFile("text.tsv").writeText(out.toString())
    }

    @Test
    fun screenshotPath() = screenshots("screenshot.tsv") { ConversationTurns.split(it) }

    /** 한 문장을 어절 2개씩 말풍선으로 나눠 보낸 대화의 스크린샷 */
    @Test
    fun screenshotSplitBubblesPath() = screenshots("screenshot_split.tsv") { text ->
        text.split('\n').flatMap { line -> line.split(' ').filter { it.isNotBlank() }.chunked(2).map { it.joinToString(" ") } }
    }

    private fun screenshots(outName: String, bubbles: (String) -> List<String>) = runBlocking {
        val repo = DetectionRepository(context)
        val ocr = MlKitOcrService()
        val dir = File(context.cacheDir, "pipeline_shots").apply { mkdirs() }
        val out = StringBuilder("id\tkind\tsource\tlevel\tscore\tocr\n")
        cases().forEach { c ->
            val png = File(dir, "${c.id}.png")
            renderChat(bubbles(c.text), png)
            val extracted = ScreenTextCleaner.clean(ocr.extractText(context, listOf(Uri.fromFile(png))))
            val r = repo.analyze(extracted)
            out.append("${c.id}\t${c.kind}\t${c.source}\t${if (extracted.isBlank()) "NO_TEXT" else r.riskLevel.name}\t${r.score}\t${extracted.replace("\n", " / ")}\n")
            png.delete()
        }
        outFile(outName).writeText(out.toString())
    }

    /** 메신저 대화 화면처럼 그린다: 상태표시줄, 상대 말풍선(왼쪽), 말풍선 옆 시각. */
    private fun renderChat(turns: List<String>, file: File) {
        val width = 1080
        val bubbleMax = 720
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 44f }
        val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 30f }
        val layouts = turns.map { t ->
            StaticLayout.Builder.obtain(t, 0, t.length, textPaint, bubbleMax - 60)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        }
        val height = 200 + layouts.sumOf { it.height + 90 } + 60
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(186, 206, 224)) // 카카오톡 비슷한 배경
        canvas.drawText("SKT 10:24", 40f, 60f, smallPaint)
        canvas.drawText("100%", width - 120f, 60f, smallPaint)
        canvas.drawText("← 상대방", 40f, 150f, textPaint)
        var y = 200f
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        layouts.forEachIndexed { i, layout ->
            val w = (0 until layout.lineCount).maxOf { layout.getLineWidth(it) } + 60
            canvas.drawRoundRect(RectF(40f, y, 40f + w, y + layout.height + 40), 28f, 28f, bubblePaint)
            canvas.save(); canvas.translate(70f, y + 20); layout.draw(canvas); canvas.restore()
            canvas.drawText("오전 10:${(18 + i).toString().padStart(2, '0')}", 60f + w, y + layout.height + 36, smallPaint)
            y += layout.height + 90
        }
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }
}
