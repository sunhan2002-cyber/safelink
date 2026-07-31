package com.safelink.app.data.ocr

import android.content.Context
import android.net.Uri

/**
 * 스크린샷 → 텍스트 추출(OCR) 추상화.
 *
 * 실제 OCR(ML Kit Text Recognition 한국어) 연결 전까지의 **임시 구조**다.
 * 화면·분석 흐름은 이 인터페이스에만 의존하므로, 나중에 [StubOcrService] 를
 * ML Kit 구현체(MlKitOcrService)로 갈아끼우기만 하면 UI/분석 코드는 그대로 동작한다.
 *
 * ┌ OCR 교체 지점 ─────────────────────────────────────────────┐
 * │ 1. build.gradle: com.google.mlkit:text-recognition-korean 추가 │
 * │ 2. MlKitOcrService 구현:                                        │
 * │    images.forEach { InputImage.fromFilePath(ctx, uri)          │
 * │      -> recognizer.process(img) -> text }                      │
 * │ 3. DetectionViewModel 의 ocrService 를 교체                      │
 * └───────────────────────────────────────────────────────────────┘
 *
 * 참고: 실제 ML Kit 는 비동기(Task/suspend)라 연결 시 이 함수를 suspend 로 바꾸고
 * ViewModel 에서 viewModelScope + 로딩 상태로 감싼다. 지금은 임시(동기)로 둔다.
 */
interface OcrService {
    /** 여러 스크린샷에서 텍스트를 추출해 하나의 문자열로 합쳐 반환. */
    fun extractText(context: Context, images: List<Uri>): String
}
