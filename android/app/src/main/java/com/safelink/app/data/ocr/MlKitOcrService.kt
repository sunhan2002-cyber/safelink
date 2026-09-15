package com.safelink.app.data.ocr

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 실제 스크린샷 OCR 구현 — Google ML Kit 한국어 텍스트 인식(온디바이스).
 *
 * - 모델이 앱에 번들되어 **오프라인·기기 내부**에서 동작한다(Design.md 최소 수집/기기 내 분석 원칙).
 * - 선택한 스크린샷 각각에서 텍스트를 추출해 순서대로 합쳐 하나의 원문으로 만든다.
 * - 실패하거나 글자가 없는 이미지는 건너뛴다. 전부 실패하면 빈 문자열을 반환한다.
 *
 * ML Kit 의 [com.google.android.gms.tasks.Task] 를 코루틴에서 기다리기 위해
 * [suspendCancellableCoroutine] 로 감싼다(별도 play-services-coroutines 의존성 없이).
 */
class MlKitOcrService : OcrService {

    private val recognizer =
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    override suspend fun extractText(context: Context, images: List<Uri>): String =
        withContext(Dispatchers.IO) {
            images.mapNotNull { uri ->
                runCatching { recognizeFromUri(context, uri) }
                    .onFailure { Log.w(TAG, "OCR 실패(이미지 건너뜀): $uri", it) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
            }.joinToString(separator = "\n\n")
        }

    private suspend fun recognizeFromUri(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val text: Text = recognizer.process(image).await()
        // ML Kit 의 text.text 는 말풍선 안에서 폭 때문에 접힌 줄까지 줄바꿈으로 나눈다("전부 뿌 / 린다").
        // 탐지 규칙은 한 메시지(한 줄) 안에서 표현을 찾으므로, 접힌 줄이 끊기면 텍스트로는 잡히는 대화를 놓쳤다.
        // 블록(대개 말풍선 하나) 단위로 줄을 다시 이어 붙인다.
        // 블록 순서는 화면 위→아래가 보장되지 않는다. 짧은 말풍선이 여러 개면 순서가 뒤섞여("얘기하자" / "여기선 텔레그램으로만")
        // 나눠 보낸 문장을 잇지 못했다(기기 측정 70/106). 화면 위치(위, 왼쪽) 순으로 정렬한다.
        return text.textBlocks
            .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
            .joinToString("\n") { block -> OcrLineJoiner.join(block.lines.map { it.text }) }
    }

    /** ML Kit Task 를 코루틴 suspend 로 변환 */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
            addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
            addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
            addOnCanceledListener { cont.cancel() }
        }

    companion object {
        private const val TAG = "MlKitOcrService"
    }
}
