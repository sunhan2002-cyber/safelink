package com.safelink.app.ui.screens.detection

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safelink.app.background.BackgroundDetectionState
import com.safelink.app.data.link.LinkRiskChecker
import com.safelink.app.data.link.LinkRiskResult
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.ocr.MlKitOcrService
import com.safelink.app.data.ocr.OcrService
import com.safelink.app.data.local.RecordSource
import com.safelink.app.data.repository.DetectionRepository
import com.safelink.app.data.repository.RecordRepository
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
 * 원문(originalText)은 서버로 전송되지 않는다. 다만 기록 화면에서 판정 근거를 다시 확인할 수
 * 있어야 하므로, 분석 결과와 함께 기기 내 DB에만 저장한다([RecordRepository], Task 7.1).
 * 사용자는 설정 > "데이터 모두 삭제"로 언제든 전부 지울 수 있다.
 */
class DetectionViewModel(application: Application) : AndroidViewModel(application) {

    enum class AnalysisSource(val label: String) {
        TEXT("텍스트 입력"),
        SCREENSHOT("스크린샷 분석"),
        BACKGROUND("백그라운드 감지")
    }

    /** 원문 텍스트 — 용어 통일본(김선한 03) 기준 이번 주 핵심 입력값 */
    var originalText by mutableStateOf("")

    /** 입력 방식 — "텍스트 입력" | "스크린샷 업로드" (통일본 inputMethod) */
    var inputMethod by mutableStateOf("텍스트 입력")

    /** 스크린샷 업로드 모드에서 선택한 이미지들 (최대 10장) */
    var selectedImages by mutableStateOf<List<Uri>>(emptyList())
        private set

    var result by mutableStateOf<DetectionResult?>(null)
        private set

    /** 마지막 분석 입력 경로 — 결과 화면에서 사용자에게 어떤 경로로 들어온 결과인지 보여준다. */
    var lastAnalysisSource by mutableStateOf(AnalysisSource.TEXT)
        private set

    /**
     * 스크린샷 OCR 에서 인식된 텍스트가 없을 때 true.
     * 입력 화면에서 "텍스트를 찾지 못함" 안내를 띄우는 데 쓴다.
     */
    var ocrNoText by mutableStateOf(false)
        private set

    /** OCR 실패/무텍스트/너무 짧음 등 현재 사용자에게 보여줄 안내 문구 */
    var ocrFeedbackMessage by mutableStateOf<String?>(null)
        private set

    /**
     * 원문에 섞여 있던 링크의 안전성 검사 결과.
     *
     * 키워드 분석과 **독립적으로** 채워진다 — 검사가 실패해도(키 미설정·목록 미준비 등)
     * 위험도 판정은 그대로 나오고 이 목록만 비거나 "검사하지 못함"으로 남는다.
     */
    var linkResults by mutableStateOf<List<LinkRiskResult>>(emptyList())
        private set

    /** 링크 검사 진행 중 여부 */
    var isCheckingLinks by mutableStateOf(false)
        private set

    /** 2차 AI 보조 분석 호출 진행 중 여부 - 결과 화면에서 "정밀 분석 중" 같은 표시에 쓸 수 있음 */
    var isEscalatingToAI by mutableStateOf(false)
        private set

    private val repository: DetectionRepository by lazy { DetectionRepository(getApplication()) }
    private val recordRepository: RecordRepository by lazy { RecordRepository(getApplication()) }
    // 실제 온디바이스 OCR. (OCR 없이 흐름만 볼 땐 StubOcrService() 로 교체)
    private val ocrService: OcrService = MlKitOcrService()
    // 링크 검사 — 온디바이스 차단 목록 대조. 검사할 URL 이 외부로 나가지 않는다(LinkRiskChecker KDoc 참고)
    private val linkRiskChecker: LinkRiskChecker by lazy { LinkRiskChecker(getApplication()) }

    /** 세션(대화방) 식별자 - ViewModel 생존 기간 동안 고정. 서버 호출 시 session_id로 사용. */
    private val sessionId: String = UUID.randomUUID().toString()

    fun addImages(uris: List<Uri>) {
        selectedImages = (selectedImages + uris).distinct().take(MAX_IMAGES)
        ocrNoText = false
        ocrFeedbackMessage = null
    }

    fun removeImage(uri: Uri) {
        selectedImages = selectedImages - uri
        ocrNoText = false
        ocrFeedbackMessage = null
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
        linkResults = emptyList()
        ocrNoText = false
        ocrFeedbackMessage = null
        lastAnalysisSource = AnalysisSource.TEXT
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
        linkResults = emptyList()
        ocrNoText = false
        ocrFeedbackMessage = null
    }

    fun switchToTextInput() {
        switchMode("텍스트 입력")
    }

    /**
     * 백그라운드 감지에서 나온 최신 전체 결과를 결과 화면에 싣는다.
     * 알림 → 대응 가이드 → "분석 결과 자세히 보기" 흐름에서 호출한다.
     * 직전에 다른(텍스트/스크린샷) 결과가 남아 있어도 백그라운드 결과로 덮어써 혼동을 막는다.
     * @return 실을 백그라운드 결과가 있으면 true.
     */
    fun loadBackgroundResult(): Boolean {
        val bg = BackgroundDetectionState.latestResult.value ?: return false
        result = bg
        originalText = bg.originalText
        lastAnalysisSource = AnalysisSource.BACKGROUND
        checkLinks(bg.originalText)
        return true
    }

    /**
     * 기록 탭에서 과거 기록을 다시 여는 경로.
     *
     * 기록에는 원문이 저장돼 있으므로, 같은 온디바이스 엔진으로 다시 분석해 결과 화면을 그대로
     * 복원한다(근거·매칭 구간까지 동일하게 재현). 재분석이므로 새 기록은 남기지 않는다.
     *
     * @return 해당 기록을 찾아 복원했으면 true
     */
    suspend fun loadRecord(recordId: String): Boolean {
        val record = recordRepository.findById(recordId) ?: return false
        val text = record.originalText?.takeIf { it.isNotBlank() } ?: return false
        originalText = text
        result = repository.analyze(text)
        checkLinks(text)
        lastAnalysisSource = when (record.sourceLabel) {
            AnalysisSource.SCREENSHOT.label -> AnalysisSource.SCREENSHOT
            AnalysisSource.BACKGROUND.label -> AnalysisSource.BACKGROUND
            else -> AnalysisSource.TEXT
        }
        return true
    }

    /**
     * 분석 실행 — 스크린샷 모드면 먼저 OCR 로 사진에서 텍스트를 추출한 뒤 분석한다.
     * Analyzing 화면의 코루틴에서 호출한다(ML Kit 가 비동기라 suspend).
     *
     * @return 결과 화면으로 진행할지 여부. 스크린샷에서 텍스트를 찾지 못하면 false
     *         ([ocrNoText] = true 로 두고 입력 화면에서 안내).
     */
    suspend fun runAnalysis(): Boolean {
        ocrFeedbackMessage = null
        if (inputMethod == "스크린샷 업로드") {
            val extracted = ocrService.extractText(getApplication(), selectedImages)
            originalText = extracted
            if (extracted.isBlank()) {
                ocrNoText = true
                ocrFeedbackMessage = "스크린샷에서 텍스트를 찾지 못했어요. 글자가 선명한 이미지를 다시 선택하거나 텍스트 입력으로 분석해 주세요."
                result = null
                return false
            }
            if (extracted.replace("\\s".toRegex(), "").length < MIN_OCR_TEXT_LENGTH) {
                ocrNoText = true
                ocrFeedbackMessage = "추출된 텍스트가 너무 짧아 정확한 분석이 어려워요. 다른 스크린샷을 선택하거나 텍스트 입력으로 다시 시도해 주세요."
                result = null
                return false
            }
            lastAnalysisSource = AnalysisSource.SCREENSHOT
        } else {
            lastAnalysisSource = AnalysisSource.TEXT
        }

        // 1차 온디바이스 분석 (항상 동기, 즉시 완료) — 결과를 먼저 반영
        val onDeviceResult = repository.analyze(originalText)
        result = onDeviceResult

        // 링크 안전성 검사 — 결과 화면을 붙잡지 않도록 비동기로 돌리고 끝나는 대로 반영한다.
        checkLinks(originalText)

        // 검사 기록 저장 (Task 7.1) — 기기 내 DB에만 남으며 서버로 나가지 않는다.
        // AI 보정 이전의 온디바이스 결과를 기준으로 저장한다(보정은 비동기라 시점이 늦음).
        viewModelScope.launch {
            runCatching {
                recordRepository.saveDetection(
                    result = onDeviceResult,
                    source = if (lastAnalysisSource == AnalysisSource.SCREENSHOT) {
                        RecordSource.SCREENSHOT
                    } else {
                        RecordSource.TEXT_INPUT
                    }
                )
            }
        }

        // 2차 AI 보조 분석: 조건 충족 시 비동기로 호출해 result 를 한 번 더 갱신(신기훈).
        // runAnalysis 는 온디바이스 결과가 나오면 바로 반환하고, AI 보정은 이후 자연스럽게 들어온다.
        // 결과 화면은 viewModel.result 를 구독하므로 자동 재구성된다. 네트워크 실패 시 escalateToAI 가
        // 온디바이스 결과를 그대로 반환하므로 result 가 나빠지는 경우는 없음.
        //
        // ⚠️ 한계: 단일 입력 구조라 recentTurns=[originalText] 고정. 실제 다중 턴 세션 추적이
        //   생기면 누적 상태로 교체할 것 (07번 문서 "recentTurns 한계" 참고). shouldEscalateToAI의
        //   AI 호출 판단 자체는 5주차 정리로 세션 턴 수와 무관해졌음(09번 문서 참고) —
        //   sessionTurnCount 파라미터는 더 이상 없음.
        if (repository.shouldEscalateToAI(onDeviceResult)) {
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

    /**
     * 원문에서 링크를 뽑아 안전성을 검사한다.
     *
     * 링크가 없으면 아무 일도 하지 않는다. 검사는 화면 전환을 막지 않도록 항상 비동기로 돌리고,
     * 실패하면 조용히 빈 목록으로 둔다 — 링크 검사는 부가 정보이지 분석의 전제가 아니다.
     */
    private fun checkLinks(text: String) {
        linkResults = emptyList()
        if (!linkRiskChecker.isConfigured) return
        viewModelScope.launch {
            isCheckingLinks = true
            linkResults = runCatching { linkRiskChecker.checkText(text) }.getOrDefault(emptyList())
            isCheckingLinks = false
        }
    }

    companion object {
        const val MAX_IMAGES = 10
        private const val MIN_OCR_TEXT_LENGTH = 8
    }
}
