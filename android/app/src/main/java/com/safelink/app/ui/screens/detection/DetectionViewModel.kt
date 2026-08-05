package com.safelink.app.ui.screens.detection

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.ocr.OcrService
import com.safelink.app.data.ocr.StubOcrService
import com.safelink.app.data.repository.DetectionRepository

/**
 * 대화 분석 공유 ViewModel — DetectionInput -> Analyzing -> DetectionResult 세 화면이
 * NavGraph 범위에서 같은 인스턴스를 공유한다 (docs/AndroidStructure.md 인자 전달 규칙:
 * 복합 객체는 라우트로 넘기지 않고 공유 ViewModel로 전달).
 *
 * 분석 로직은 [DetectionRepository]/[com.safelink.app.data.repository.DetectionEngine]로
 * 위임한다 (반복감쇠, 조합 보너스, standalone_recommend 게이팅까지 포함된 v1 엔진 —
 * 신기훈 4주차 결과물, `주차별_결과물/4주차_결과물_신기훈/` 참고). Hilt를 아직 안 쓰는
 * ViewModel이라 생성자를 직접 호출한다.
 *
 * 원문(originalText)은 세션 메모리에만 유지하고 저장하지 않는다 (Design.md 최소 수집 원칙).
 */
class DetectionViewModel(application: Application) : AndroidViewModel(application) {

    /** 원문 텍스트 — 용어 통일본(김선한 03) 기준 이번 주 핵심 입력값 */
    var originalText by mutableStateOf("")

    /** 입력 방식 — "텍스트 입력" | "스크린샷 업로드" (통일본 inputMethod) */
    var inputMethod by mutableStateOf("텍스트 입력")

    /** 스크린샷 업로드 모드에서 선택한 이미지들 (최대 10장) */
    var selectedImages by mutableStateOf<List<Uri>>(emptyList())
        private set

    var result by mutableStateOf<DetectionResult?>(null)
        private set

    private val repository: DetectionRepository by lazy { DetectionRepository(getApplication()) }
    private val ocrService: OcrService = StubOcrService()

    fun addImages(uris: List<Uri>) {
        selectedImages = (selectedImages + uris).distinct().take(MAX_IMAGES)
    }

    fun removeImage(uri: Uri) {
        selectedImages = selectedImages - uri
    }

    /**
     * 새 분석 세션 시작 — 입력·이미지·결과를 모두 비운다.
     * 홈에서 "대화 분석" 진입, 결과 화면 "다시 분석하기" 등 새 흐름을 시작할 때 호출.
     * (공유 ViewModel이 NavGraph 범위로 살아있어 초기화하지 않으면 이전 세션 값이 남는다)
     */
    fun reset() {
        originalText = ""
        inputMethod = "텍스트 입력"
        selectedImages = emptyList()
        result = null
    }

    /**
     * 입력 방식 전환 — 이전 모드의 입력을 비워 텍스트/스크린샷 데이터가 섞이지 않게 한다.
     * (텍스트로 분석한 뒤 스크린샷 모드로 넘어가도 이전 텍스트/결과가 따라오지 않도록)
     */
    fun switchMode(mode: String) {
        if (inputMethod == mode) return
        inputMethod = mode
        originalText = ""
        selectedImages = emptyList()
        result = null
    }

    /**
     * 스크린샷 → OCR(임시 구조) → originalText 채움. 스크린샷 모드에서 분석 시작 직전 호출.
     * 실제 ML Kit 연결 시 suspend + viewModelScope + 로딩 상태로 감싼다(OcrService KDoc 참고).
     */
    fun runOcrOnSelectedImages() {
        originalText = ocrService.extractText(getApplication(), selectedImages)
    }

    /** 원문 텍스트를 분석해 result 에 반영한다. Analyzing 화면 진입 후 호출. */
    fun analyze() {
        result = repository.analyze(originalText)
    }

    companion object {
        const val MAX_IMAGES = 10
    }
}
