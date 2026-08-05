package com.safelink.app.ui.screens.detection

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.ocr.OcrService
import com.safelink.app.data.ocr.StubOcrService
import com.safelink.app.data.repository.DetectionRepository
import kotlinx.coroutines.launch
import java.util.UUID

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

    /** 2차 AI 보조 분석 호출 진행 중 여부 - 결과 화면에서 "정밀 분석 중" 같은 표시에 쓸 수 있음 */
    var isEscalatingToAI by mutableStateOf(false)
        private set

    private val repository: DetectionRepository by lazy { DetectionRepository(getApplication()) }
    private val ocrService: OcrService = StubOcrService()

    /** 세션(대화방) 식별자 - ViewModel 생존 기간 동안 고정. 서버 호출 시 session_id로 사용. */
    private val sessionId: String = UUID.randomUUID().toString()

    fun addImages(uris: List<Uri>) {
        selectedImages = (selectedImages + uris).distinct().take(MAX_IMAGES)
    }

    fun removeImage(uri: Uri) {
        selectedImages = selectedImages - uri
    }

    /**
     * 스크린샷 → OCR(임시 구조) → originalText 채움. 스크린샷 모드에서 분석 시작 직전 호출.
     * 실제 ML Kit 연결 시 suspend + viewModelScope + 로딩 상태로 감싼다(OcrService KDoc 참고).
     */
    fun runOcrOnSelectedImages() {
        originalText = ocrService.extractText(getApplication(), selectedImages)
    }

    /**
     * 원문 텍스트를 분석해 result 에 반영한다. Analyzing 화면 진입 후 호출.
     *
     * 온디바이스 분석(항상 동기, 즉시 완료) 결과를 먼저 반영하고, [DetectionRepository.shouldEscalateToAI]
     * 조건에 해당하면 그 뒤에 비동기로 2차 AI 보조 분석을 호출해서 result를 한 번 더 갱신한다.
     * 서버 호출이 느리거나 실패해도 화면에는 이미 온디바이스 결과가 떠 있는 상태 — AI 보정은
     * "나중에 slight 업데이트"로 자연스럽게 들어온다. 네트워크 실패 시 escalateToAI가 온디바이스
     * 결과를 그대로 반환하므로 result가 나빠지는 경우는 없음.
     *
     * TODO: sessionTurnCount/recentTurns는 지금 "입력 1건 = 세션 1턴" 기준 단순화된 값.
     * 실제 다중 턴 세션 추적(메시지 여러 개 누적)이 생기면 이 부분을 그 상태로 교체할 것.
     */
    fun analyze() {
        val onDeviceResult = repository.analyze(originalText)
        result = onDeviceResult

        if (repository.shouldEscalateToAI(onDeviceResult, sessionTurnCount = 1)) {
            viewModelScope.launch {
                isEscalatingToAI = true
                result = repository.escalateToAI(
                    result = onDeviceResult,
                    sessionId = sessionId,
                    recentTurns = listOf(originalText)
                )
                isEscalatingToAI = false
            }
        }
    }

    companion object {
        const val MAX_IMAGES = 10
    }
}
