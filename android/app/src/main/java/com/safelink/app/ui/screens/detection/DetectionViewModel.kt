package com.safelink.app.ui.screens.detection

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.ocr.MlKitOcrService
import com.safelink.app.data.ocr.OcrService
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

    /**
     * 스크린샷 OCR 에서 인식된 텍스트가 없을 때 true.
     * 입력 화면에서 "텍스트를 찾지 못함" 안내를 띄우는 데 쓴다.
     */
    var ocrNoText by mutableStateOf(false)
        private set

    /** 2차 AI 보조 분석 호출 진행 중 여부 - 결과 화면에서 "정밀 분석 중" 같은 표시에 쓸 수 있음 */
    var isEscalatingToAI by mutableStateOf(false)
        private set

    private val repository: DetectionRepository by lazy { DetectionRepository(getApplication()) }
    // 실제 온디바이스 OCR. (OCR 없이 흐름만 볼 땐 StubOcrService() 로 교체)
    private val ocrService: OcrService = MlKitOcrService()

    /** 세션(대화방) 식별자 - ViewModel 생존 기간 동안 고정. 서버 호출 시 session_id로 사용. */
    private val sessionId: String = UUID.randomUUID().toString()

    fun addImages(uris: List<Uri>) {
        selectedImages = (selectedImages + uris).distinct().take(MAX_IMAGES)
        ocrNoText = false
    }

    fun removeImage(uri: Uri) {
        selectedImages = selectedImages - uri
        ocrNoText = false
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
        ocrNoText = false
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
        ocrNoText = false
    }

    /**
     * 분석 실행 — 스크린샷 모드면 먼저 OCR 로 사진에서 텍스트를 추출한 뒤 분석한다.
     * Analyzing 화면의 코루틴에서 호출한다(ML Kit 가 비동기라 suspend).
     *
     * @return 결과 화면으로 진행할지 여부. 스크린샷에서 텍스트를 찾지 못하면 false
     *         ([ocrNoText] = true 로 두고 입력 화면에서 안내).
     */
    suspend fun runAnalysis(): Boolean {
        if (inputMethod == "스크린샷 업로드") {
            val extracted = ocrService.extractText(getApplication(), selectedImages)
            originalText = extracted
            if (extracted.isBlank()) {
                ocrNoText = true
                result = null
                return false
            }
        }

        // 1차 온디바이스 분석 (항상 동기, 즉시 완료) — 결과를 먼저 반영
        val onDeviceResult = repository.analyze(originalText)
        result = onDeviceResult

        // 2차 AI 보조 분석: 조건 충족 시 비동기로 호출해 result 를 한 번 더 갱신(신기훈).
        // runAnalysis 는 온디바이스 결과가 나오면 바로 반환하고, AI 보정은 이후 자연스럽게 들어온다.
        // 결과 화면은 viewModel.result 를 구독하므로 자동 재구성된다. 네트워크 실패 시 escalateToAI 가
        // 온디바이스 결과를 그대로 반환하므로 result 가 나빠지는 경우는 없음.
        //
        // ⚠️ 한계: 단일 입력 구조라 sessionTurnCount=1, recentTurns=[originalText] 고정.
        //   실제 다중 턴 세션 추적이 생기면 누적 상태로 교체할 것 (07번 문서 "recentTurns 한계" 참고).
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
        return true
    }

    companion object {
        const val MAX_IMAGES = 10
    }
}
