package com.safelink.app.data.ocr

import android.content.Context
import android.net.Uri

/**
 * OCR 연결 전까지의 임시 구현.
 *
 * 실제 텍스트 추출 대신, 선택된 스크린샷이 있으면 시연용 예시 텍스트를 반환한다.
 * → 스크린샷 분석 "흐름"(선택 → 추출 → 분석 → 결과)을 앱 안에서 확인할 수 있게 해준다.
 * ML Kit 연결 시 이 클래스를 MlKitOcrService 로 교체하면 된다.
 */
class StubOcrService : OcrService {

    override fun extractText(context: Context, images: List<Uri>): String {
        // TODO(OCR 연결): ML Kit KoreanTextRecognizer 로 각 이미지에서 실제 텍스트 추출.
        if (images.isEmpty()) return ""
        return SAMPLE_EXTRACTED_TEXT
    }

    companion object {
        /** 시연용: 보이스피싱 예시 대화 (실제 OCR 결과 자리) */
        const val SAMPLE_EXTRACTED_TEXT =
            "택배기사인데요, 배송 중 확인 차 연락드렸습니다. 그럼 명의 도용 우려가 있어서 " +
                "확인이 필요합니다. 지금 당장 확인 안 하시면 계좌가 압류될 수 있습니다."
    }
}
