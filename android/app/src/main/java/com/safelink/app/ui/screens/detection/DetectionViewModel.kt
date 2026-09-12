package com.safelink.app.ui.screens.detection

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safelink.app.background.BackgroundDetectionState
import com.safelink.app.data.link.LinkExtractor
import com.safelink.app.data.link.LinkResultCodec
import com.safelink.app.data.link.LinkRiskChecker
import com.safelink.app.data.link.LinkRiskPolicy
import com.safelink.app.data.link.LinkRiskResult
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.ocr.MlKitOcrService
import com.safelink.app.data.ocr.OcrService
import com.safelink.app.data.local.RecordFeedback
import com.safelink.app.data.local.RecordSource
import com.safelink.app.data.repository.ConversationTurns
import com.safelink.app.data.repository.DetectionRepository
import com.safelink.app.data.repository.RecordRepository
import com.safelink.app.settings.AiConsentStore
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
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

    /**
     * 분석이 낸 기준 판정 — 온디바이스 결과, AI 보조분석이 들어오면 그 결과.
     * 화면에는 여기에 링크 판정을 얹은 [result] 를 보여준다.
     */
    private var analyzed by mutableStateOf<DetectionResult?>(null)

    /**
     * 결과 화면에 보여줄 최종 판정 — 기준 판정에 링크 판정을 얹는다([LinkRiskPolicy]).
     *
     * 예전에는 링크가 악성으로 확인돼도 키워드가 없으면 "안전 · 0점"으로 보였다(바로 아래 링크 카드는
     * "피싱·사칭 사이트"). 링크 검사와 AI 보조분석은 따로 끝나므로, 합친 값을 저장해 두지 않고 읽을 때마다
     * 합쳐 어느 쪽이 먼저 끝나도 화면이 맞게 한다.
     */
    val result: DetectionResult?
        get() = analyzed?.let { LinkRiskPolicy.apply(it, linkResults) }

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
     * 위험한 링크가 확인되면 [result] 의 위험도가 긴급으로 올라간다.
     */
    var linkResults by mutableStateOf<List<LinkRiskResult>>(emptyList())
        private set

    /** 링크 검사 진행 중 여부 */
    var isCheckingLinks by mutableStateOf(false)
        private set

    /** 2차 AI 보조 분석 호출 진행 중 여부 - 결과 화면에서 "정밀 분석 중" 같은 표시에 쓸 수 있음 */
    var isEscalatingToAI by mutableStateOf(false)
        private set

    /** "AI 보조분석 요청"(수동 신고)이 실패했을 때 결과 화면에 보여줄 안내 */
    var manualAiMessage by mutableStateOf<String?>(null)
        private set

    /**
     * 사용자가 이 결과에 남긴 피드백(맞음/오탐). 남기지 않았으면 null.
     * 오탐 신고를 모아 두면 어떤 표현·점수 구간에서 헛짚는지 확인할 근거가 된다(기기 안에만 저장).
     */
    var feedback by mutableStateOf<RecordFeedback?>(null)
        private set

    /** 지금 결과 화면의 결과가 저장된 기록 id — AI 보정이 나중에 들어오면 이 기록을 갱신한다. */
    private var currentRecordId: String? = null

    /**
     * 화면에 올린 결과의 순번. 분석·기록 열기를 새로 시작할 때마다 올린다.
     * 늦게 끝난 이전 분석의 링크 검사·AI 결과가 지금 화면을 덮어쓰지 않도록, 비동기 작업은 이 값이 그대로일 때만 화면을 바꾼다.
     */
    private var generation = 0

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
        startNewResult()
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
        startNewResult()
        ocrNoText = false
        ocrFeedbackMessage = null
    }

    fun switchToTextInput() {
        switchMode("텍스트 입력")
    }

    /** 이전 결과의 화면 상태를 비우고 순번을 올린다. 새 결과를 화면에 올리기 직전에 부른다. */
    private fun startNewResult(): Int {
        generation++
        analyzed = null
        linkResults = emptyList()
        isCheckingLinks = false
        isEscalatingToAI = false
        manualAiMessage = null
        currentRecordId = null
        feedback = null
        return generation
    }

    /**
     * 백그라운드 감지에서 나온 최신 전체 결과를 결과 화면에 싣는다.
     * 알림 → 대응 가이드 → "분석 결과 자세히 보기" 흐름에서 호출한다.
     * 직전에 다른(텍스트/스크린샷) 결과가 남아 있어도 백그라운드 결과로 덮어써 혼동을 막는다.
     * @return 실을 백그라운드 결과가 있으면 true.
     */
    fun loadBackgroundResult(): Boolean {
        val bg = BackgroundDetectionState.latestResult.value ?: return false
        val gen = startNewResult()
        analyzed = bg
        originalText = bg.originalText
        lastAnalysisSource = AnalysisSource.BACKGROUND
        viewModelScope.launch { showLinks(gen, bg.originalText) }
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
        val onDevice = repository.analyze(text)
        val gen = startNewResult()
        originalText = text
        // 위험도·점수·유형은 기록에 저장된 판정을 그대로 쓴다 — 기록 목록과 상세 보기가 같은 판정을 보여야 한다.
        // 재분석은 근거(매칭 구간·규칙 설명)를 되살리는 용도다. 재분석 값을 그대로 쓰면
        // - AI 보조분석이 반영된 기록은 AI 반영 전 점수로 보이고(재분석은 온디바이스로만 한다),
        // - 백그라운드에서 악성 링크로 알린 기록은 키워드가 없어 "안전"으로 보였다(목록은 "긴급").
        val restored = onDevice.copy(
            score = record.score,
            riskLevel = record.riskLevel,
            category = record.category.ifBlank { onDevice.category },
            aiSummary = record.aiSummary,
            aiDetectedPattern = record.aiDetectedPattern
        )
        analyzed = restored
        currentRecordId = recordId
        feedback = record.feedback
        lastAnalysisSource = when (record.sourceLabel) {
            AnalysisSource.SCREENSHOT.label -> AnalysisSource.SCREENSHOT
            AnalysisSource.BACKGROUND.label -> AnalysisSource.BACKGROUND
            else -> AnalysisSource.TEXT
        }
        viewModelScope.launch {
            val links = showLinks(gen, text, stored = LinkResultCodec.decode(record.linkResultsJson))
            // 저장한 뒤에 위험으로 등재된 링크가 새로 확인되면 기록의 판정도 함께 올린다.
            VerdictRecorder(recordRepository, recordId, restored).onLinks(links)
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
                startNewResult()
                return false
            }
            if (extracted.replace("\\s".toRegex(), "").length < MIN_OCR_TEXT_LENGTH) {
                ocrNoText = true
                ocrFeedbackMessage = "추출된 텍스트가 너무 짧아 정확한 분석이 어려워요. 다른 스크린샷을 선택하거나 텍스트 입력으로 다시 시도해 주세요."
                startNewResult()
                return false
            }
            lastAnalysisSource = AnalysisSource.SCREENSHOT
        } else {
            lastAnalysisSource = AnalysisSource.TEXT
        }

        // 1차 온디바이스 분석 (항상 동기, 즉시 완료) — 결과를 먼저 반영.
        // 붙여넣은 대화는 줄 단위로 턴을 나눠 분석한다 — 반복·장기세션 규칙은 턴이 나뉘어야 발동한다.
        val turns = ConversationTurns.split(originalText)
        val onDeviceResult = repository.analyzeTurns(turns)
        val gen = startNewResult()
        analyzed = onDeviceResult
        val text = originalText
        val turnsForAi = ConversationTurns.recentForAi(turns)

        // 2차 AI 보조 분석: 조건 충족 시 비동기로 호출해 결과를 한 번 더 갱신(신기훈).
        // 네트워크 실패 시 escalateToAI 가 온디바이스 결과를 그대로 반환하므로 결과가 나빠지는 경우는 없음.
        // ⚠️ 한계: 단일 입력 구조라 recentTurns=[originalText] 고정 (07번 문서 "recentTurns 한계" 참고).
        // 자동 호출은 사용자가 동의한 경우에만 한다 — 예전에는 회색지대 점수면 안내 없이 전송됐다.
        // 동의 전이라도 결과 화면의 "AI 보조분석 요청"으로 한 건씩 직접 받을 수 있다([requestManualAi]).
        val shouldEscalate = repository.shouldEscalateToAI(onDeviceResult) &&
            AiConsentStore.isManualEnabled(getApplication())
        isEscalatingToAI = shouldEscalate

        // 링크 안전성 검사 — 기기 안의 차단 목록과 대조해 대개 금방 끝난다. 위험한 링크가 있으면 판정이 긴급으로
        // 바뀌므로, 결과 화면이 "안전"으로 떴다가 곧 "긴급"으로 바뀌지 않게 잠깐([LINK_WAIT_MS]) 기다렸다 넘어간다.
        // 그보다 오래 걸리면 기다리지 않고 넘어가고, 끝나는 대로 화면과 기록에 반영된다.
        val links = viewModelScope.async { showLinks(gen, text) }
        withTimeoutOrNull(LINK_WAIT_MS) { links.await() }

        // 검사 기록 저장 (Task 7.1) — 기기 내 DB에만 남으며 서버로 나가지 않는다.
        // 온디바이스 결과를 먼저 저장하고, 링크 판정·AI 보조분석이 들어오면 같은 기록을 최종 판정으로 갱신한다.
        // (예전에는 AI 보정 전 결과만 남아, 결과 화면은 "긴급"인데 기록은 "경고"로 보이는 불일치가 있었다.)
        val source = if (lastAnalysisSource == AnalysisSource.SCREENSHOT) RecordSource.SCREENSHOT else RecordSource.TEXT_INPUT
        viewModelScope.launch {
            val recordId = runCatching { recordRepository.saveDetection(onDeviceResult, source) }.getOrNull()
            if (gen == generation) currentRecordId = recordId
            val recorder = VerdictRecorder(recordRepository, recordId, onDeviceResult)
            launch { recorder.onLinks(links.await()) }
            if (shouldEscalate) {
                val refined = repository.escalateToAI(
                    result = onDeviceResult,
                    sessionId = sessionId,
                    // 대화 흐름을 함께 넘긴다 — 예전에는 전체를 한 덩어리로 1건만 넘겼다
                    recentTurns = turnsForAi
                )
                if (refined !== onDeviceResult) {
                    // 그사이 사용자가 새 분석을 시작했으면 화면은 건드리지 않고 기록만 맞춘다.
                    if (gen == generation) analyzed = refined
                    recorder.onAi(refined)
                }
                if (gen == generation) isEscalatingToAI = false
            }
        }
        return true
    }

    /**
     * 원문의 링크를 검사해 화면에 반영하고, 합친 결과를 돌려준다.
     *
     * 링크가 없으면 검사하지 않는다. 실패하면 조용히 넘어간다 — 링크 검사는 부가 정보이지 분석의 전제가 아니다.
     *
     * @param gen 이 검사를 시작한 결과의 순번. 그사이 화면이 다른 결과로 바뀌었으면 화면은 건드리지 않는다.
     * @param stored 기록에 남아 있던 당시 판정. 먼저 보여주고 새 검사 결과와 합친다([LinkResultCodec.merge]) —
     *   다시 검사하지 못해도(오프라인 등) 당시 판정이 사라지지 않는다.
     */
    private suspend fun showLinks(gen: Int, text: String, stored: List<LinkRiskResult> = emptyList()): List<LinkRiskResult> {
        if (gen == generation) linkResults = stored
        if (!linkRiskChecker.isConfigured || LinkExtractor.extract(text).isEmpty()) return stored
        if (gen == generation) isCheckingLinks = true
        val fresh = runCatching { linkRiskChecker.checkText(text) }.getOrDefault(emptyList())
        val merged = LinkResultCodec.merge(stored, fresh)
        if (gen == generation) {
            linkResults = merged
            isCheckingLinks = false
        }
        return merged
    }

    /**
     * 사용자가 결과 화면에서 "AI 보조분석 요청"을 누른 경우 (보고서 4장 "수동 신고" 조건).
     *
     * 온디바이스 판정이 AI 호출 조건(회색지대 점수, 신규 세부유형)에 걸리지 않았더라도, 사용자가
     * 애매하다고 느끼면 직접 AI 보조분석을 받을 수 있게 한다. 이전에는 이 조건을 넘기는 화면이 없어
     * manualReportFlag 가 항상 false 였다.
     */
    fun requestManualAi() {
        val base = analyzed ?: return
        if (isEscalatingToAI || base.aiSummary != null || base.aiDetectedPattern != null) return
        if (!repository.shouldEscalateToAI(base, manualReportFlag = true)) return
        val gen = generation
        val text = originalText.ifBlank { base.originalText }
        val recordId = currentRecordId
        val linksAtRequest = linkResults
        manualAiMessage = null
        isEscalatingToAI = true
        viewModelScope.launch {
            val refined = repository.escalateToAI(
                result = base,
                sessionId = sessionId,
                recentTurns = ConversationTurns.recentForAi(ConversationTurns.split(text))
            )
            val stillShown = gen == generation
            if (refined === base) {
                if (stillShown) manualAiMessage = "AI 보조분석을 받지 못했어요. 잠시 후 다시 시도해 주세요."
            } else {
                if (stillShown) analyzed = refined
                // 기록에는 화면과 같은 판정(링크 판정까지 얹은 값)을 남긴다.
                val links = if (stillShown) linkResults else linksAtRequest
                recordId?.let { id -> runCatching { recordRepository.updateVerdict(id, LinkRiskPolicy.apply(refined, links)) } }
            }
            if (stillShown) isEscalatingToAI = false
        }
    }

    /**
     * 결과 화면에서 "이 판단이 맞았나요?"에 답한 경우. 같은 값을 다시 누르면 취소된다.
     * 기록이 아직 저장되지 않았으면(저장 실패 등) 화면 표시만 바뀌고 남지 않는다.
     */
    fun submitFeedback(value: RecordFeedback) {
        val next = if (feedback == value) null else value
        feedback = next
        val recordId = currentRecordId ?: return
        viewModelScope.launch {
            runCatching { recordRepository.updateFeedback(recordId, next) }
        }
    }

    companion object {
        const val MAX_IMAGES = 10
        private const val MIN_OCR_TEXT_LENGTH = 8

        /** 결과 화면으로 넘어가기 전에 링크 검사를 기다리는 최대 시간 */
        private const val LINK_WAIT_MS = 1500L
    }
}

/**
 * 분석 한 건의 최종 판정을 기록에 남긴다.
 *
 * 링크 검사와 AI 보조분석은 따로, 순서 없이 끝난다. 각자 기록을 고치면 늦게 끝난 쪽이 먼저 끝난 쪽의
 * 반영을 지운다(예: AI 가 점수를 낮춰 쓰면서 위험 링크로 올라간 "긴급"을 덮는다). 그래서 두 결과를 여기에
 * 모아 매번 "지금까지 나온 결과를 모두 반영한 판정"을 쓰고, 쓰기는 한 번에 하나씩만 한다.
 */
private class VerdictRecorder(
    private val records: RecordRepository,
    private val recordId: String?,
    private var base: DetectionResult
) {
    private var links: List<LinkRiskResult> = emptyList()
    private val lock = Mutex()

    suspend fun onLinks(results: List<LinkRiskResult>) {
        if (results.isEmpty()) return
        lock.withLock {
            links = results
            val id = recordId ?: return
            runCatching {
                records.updateLinkResults(id, results)
                records.updateVerdict(id, LinkRiskPolicy.apply(base, links))
            }
        }
    }

    suspend fun onAi(refined: DetectionResult) {
        lock.withLock {
            base = refined
            val id = recordId ?: return
            runCatching { records.updateVerdict(id, LinkRiskPolicy.apply(base, links)) }
        }
    }
}
