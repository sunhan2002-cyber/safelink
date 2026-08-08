package com.safelink.app.data.ocr

import android.content.Context
import android.net.Uri

/**
 * 스크린샷 → 텍스트 추출(OCR) 추상화.
 *
 * 실제 구현은 [MlKitOcrService] (ML Kit Text Recognition 한국어, 온디바이스·오프라인).
 * 화면·분석 흐름은 이 인터페이스에만 의존하므로, 구현체 교체만으로 동작한다.
 * ([StubOcrService] 는 OCR 없이 흐름만 확인하는 테스트/참고용으로 남겨둔다.)
 *
 * ML Kit 는 비동기(Task) API 라 여기서는 suspend 로 노출하고, 호출부(Analyzing 화면)는
 * 코루틴에서 호출하며 진행 상태를 표시한다.
 */
interface OcrService {
    /**
     * 여러 스크린샷에서 텍스트를 추출해 하나의 문자열로 합쳐 반환.
     * 인식된 텍스트가 없으면 빈 문자열을 반환한다(호출부에서 "텍스트 없음"으로 처리).
     */
    suspend fun extractText(context: Context, images: List<Uri>): String
}
