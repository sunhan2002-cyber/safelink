package com.safelink.app.data.ocr

import android.content.Context
import android.net.Uri

/**
 * OCR 없이 흐름만 확인하는 테스트/참고용 구현.
 *
 * 실제 텍스트 추출 대신, 선택된 스크린샷이 있으면 예시 텍스트를 반환한다.
 * 실제 앱은 [MlKitOcrService] 를 사용한다(DetectionViewModel 참고).
 * OCR 모델 없이 UI/분석 흐름만 빠르게 확인하고 싶을 때 ViewModel 에서 이 구현으로 바꾼다.
 */
class StubOcrService : OcrService {

    override suspend fun extractText(context: Context, images: List<Uri>): String {
        if (images.isEmpty()) return ""
        return SAMPLE_EXTRACTED_TEXT
    }

    companion object {
        /**
         * 시연용: 스크린샷에서 추출된 것으로 가정하는 예시 대화 (실제 OCR 결과 자리).
         * 텍스트 입력 모드의 예시 문장과 "다른" 사기 유형(기관 사칭)으로 두어,
         * 스크린샷 분석이 텍스트 결과를 그대로 보여주는 게 아니라 실제로 이 문장을
         * 분석한 결과라는 걸 시연 중에 눈으로 구분할 수 있게 한다.
         */
        const val SAMPLE_EXTRACTED_TEXT =
            "서울중앙지검 수사관입니다. 본인 명의 계좌가 범죄에 연루되어 조사가 필요합니다. " +
                "지금 안전계좌로 자금을 이체하지 않으면 구속 수사가 진행됩니다. " +
                "이 사건은 비밀 유지 의무가 있으니 가족에게도 절대 알리지 마세요."
    }
}
