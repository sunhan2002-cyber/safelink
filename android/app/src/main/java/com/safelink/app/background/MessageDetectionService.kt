package com.safelink.app.background

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.local.RecordSource
import com.safelink.app.data.repository.DetectionRepository
import com.safelink.app.data.repository.RecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.safelink.app.data.link.ExtractedLink
import com.safelink.app.data.link.LinkExtractor
import com.safelink.app.data.link.LinkRiskChecker
import com.safelink.app.data.link.LinkRiskResult
import com.safelink.app.data.link.LinkVerdict
import com.safelink.app.notification.RiskNotifier
import com.safelink.app.settings.AiConsentStore
import java.util.UUID

/**
 * 백그라운드 위험 감지 서비스 — 화면 텍스트 추출 → 온디바이스 분석 → 알림까지 실제 동작.
 *
 * ── 왜 AccessibilityService 인가 ─────────────────────────────────────────
 * 안드로이드에서 다른 앱(카카오톡·문자 등)의 화면 텍스트를 백그라운드로 읽는 표준·합법 경로는
 * AccessibilityService 다. 사용자가 [설정 > 접근성]에서 SafeLink 를 직접 켜야만 동작하며,
 * 켜기 전에는 실행되지 않는다(개인정보 보호 + Play 정책 준수).
 *
 * ── 감지 → 분석 → 알림 흐름 ─────────────────────────────────────────────
 *   1. onAccessibilityEvent  : 감지 대상 메신저의 화면 변화 수신
 *   2. extractVisibleText()  : 화면 노드 트리에서 대화 텍스트 추출
 *   3. DetectionRepository.analyze(text) : 온디바이스 위험 분석 (기존 엔진 재사용)
 *   4. RiskNotifier.notifyRisk()         : 경고 이상이면 배너 알림
 *   5. (알림 탭) → 대응 가이드 / 긴급 화면 딥링크 ([RiskNotifier] + MainActivity)
 *
 * ── 성능/중복 억제 ──────────────────────────────────────────────────────
 *   - 같은 화면에서 이벤트가 쏟아지므로 [MIN_INTERVAL_MS] 간격으로만 분석(디바운스).
 *   - 직전과 동일한 텍스트는 재분석/재알림하지 않는다.
 *   - 같은 내용(앱+매칭 키워드 집합)의 반복 알림은 [isDuplicateAlert] 규칙으로 거른다 —
 *     같은 화면 스크롤은 오래 묶고, 대화방을 나갔다 다시 들어오면 다시 알린다.
 *   - 노드 순회 길이를 [MAX_CHARS] 로 제한해 과도한 처리를 막는다.
 *
 * ※ 감지 대상 패키지([MONITORED_PACKAGES])와 "감지 후 어떤 화면으로 연결할지" 기준은
 *   기능확장 담당(김선한)과 협의해 확정한다. 라우팅 매핑은 [RiskNotifier.routeFor] 한 곳에 모아둠.
 *
 * ── 개인정보: 서버로 전송되지 않음 (7주차 확인) ─────────────────────────────
 *   백그라운드 감지 경로는 [DetectionRepository.analyze]만 호출하고
 *   [DetectionRepository.escalateToAI]는 호출하지 않는다 — 즉 이 경로로 읽은 화면 텍스트는
 *   네트워크로 나갈 방법 자체가 없다(마스킹 이전에 애초에 전송 경로가 없음).
 *   화면 표시용 [BackgroundDetectionState]에는 매칭된 짧은 구간(`matchedText`)만 메모리에 두고,
 *   경고 이상으로 알림이 뜬 건은 [RecordRepository]를 통해 기기 내 DB에 기록으로 남긴다
 *   (Task 7.1 — 기록 탭 재열람용. 서버 전송 없음, 설정에서 전체 삭제 가능).
 */
class MessageDetectionService : AccessibilityService() {

    private val repository by lazy { DetectionRepository(applicationContext) }
    private val recordRepository by lazy { RecordRepository(applicationContext) }

    /** 기록 저장용 스코프 — 서비스 종료 시 함께 취소한다. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 화면 전환 직후 재확인용 — 내용이 다 그려질 때까지 잠깐 기다렸다 한 번 더 읽는다. */
    private val recheckHandler = Handler(Looper.getMainLooper())
    private val notifier by lazy { RiskNotifier(applicationContext) }

    /**
     * 링크 검사기. 검사할 주소를 외부로 보내지 않고 기기 안의 차단 목록과 대조한다
     * ([LinkRiskChecker] KDoc 참고) — 백그라운드에서 돌아가는 경로라 이 성질이 특히 중요하다.
     * 사용자가 인지하지 못한 채 화면의 링크가 밖으로 나가는 일은 없어야 한다.
     */
    private val linkRiskChecker by lazy { LinkRiskChecker(applicationContext) }

    private var lastText: String = ""
    private var lastAnalyzedAt: Long = 0L

    /** 직전에 알림을 띄운 건 — 앱과 매칭된 키워드 id 집합 */
    private var lastAlertPkg: String? = null
    private var lastAlertKeywords: Set<String>? = null
    private var lastAlertAt: Long = 0L
    /** 직전 알림이 발생한 창(화면) id — 화면이 바뀌면 새 확인 행동으로 본다 */
    private var lastAlertWindowId: Int = -1

    /** 직전에 악성 링크로 알린 주소 — 같은 주소를 반복해서 알리지 않도록 */
    private var lastLinkAlertUrl: String? = null
    private var lastLinkAlertAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in MONITORED_PACKAGES) return
        val windowId = event.windowId

        // 화면이 막 바뀐 직후에는 대화 내용이 아직 안 그려져 있을 수 있다. 그 상태로 한 번
        // 읽고 끝내면 "직전과 같은 텍스트" 규칙에 걸려 정작 내용이 채워진 뒤에는 다시 보지
        // 않는다(대화방을 열었는데 아무 반응이 없는 원인이었다). 그래서 전환 직후에는
        // 잠시 뒤 한 번 더 확인하도록 예약해 둔다.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            recheckHandler.removeCallbacksAndMessages(null)
            recheckHandler.postDelayed({ analyze(pkg, windowId, force = true) }, RECHECK_DELAY_MS)
        }

        analyze(pkg, windowId)
    }

    /**
     * @param force 화면 전환 후 예약된 재확인인지 여부. 재확인은 "내용이 다 그려졌는지 다시
     *   본다"는 의도된 한 번이므로 디바운스로 건너뛰면 안 된다(그러면 재확인이 항상 무시된다).
     */
    private fun analyze(pkg: String, windowId: Int, force: Boolean = false) {
        // 디바운스: 같은 화면 이벤트가 연속으로 오므로 일정 간격으로만 분석
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastAnalyzedAt < MIN_INTERVAL_MS) return

        // 이벤트를 보낸 앱의 화면만 읽는다. 화면 전환 직후에는 아직 이전 화면(홈 등)이
        // 잡히는 경우가 있는데, 그때 그 내용을 lastText 로 기억해 버리면 정작 대화 화면이
        // 그려진 뒤에도 "직전과 같음"으로 걸러져 한 번도 분석하지 못한다.
        // 대상 앱의 화면이 아니면 아무것도 기억하지 않고 다음 이벤트를 기다린다.
        val text = extractVisibleText(pkg) ?: return
        if (text.isBlank() || text == lastText) return

        lastText = text
        lastAnalyzedAt = now

        // 기존 온디바이스 엔진 재사용 — 실제 감지/분석은 여기서 일어난다
        val result = repository.analyze(text)

        // 알림 흐름: 경고(WARNING) 이상일 때만 상태 저장 + 배너 알림
        // (SAFE/CAUTION 등 일상 화면까지 상태·카운트에 반영하면 "오늘의 알림"이 부풀어 오르므로
        //  BackgroundDetectionState 는 실제 알림(경고 이상)만 기록한다)
        if (result.riskLevel.ordinal >= RiskLevel.WARNING.ordinal) {
            if (isDuplicateAlert(pkg, result, windowId, now)) return

            lastAlertPkg = pkg
            lastAlertKeywords = keywordsOf(result)
            lastAlertAt = now
            lastAlertWindowId = windowId

            BackgroundDetectionState.update(result, sourceApp = pkg)
            notifier.notifyRisk(
                result.riskLevel,
                result.category,
                result.matchedKeywords.firstOrNull()?.matchedText
            )
            // 검사 기록에도 남겨 "기록" 탭에서 나중에 다시 확인할 수 있게 한다 (Task 7.1).
            // 알림이 뜬 건(경고 이상)만 저장 — 일상 화면까지 기록이 쌓이지 않도록.
            // AI 보정 이전의 온디바이스 결과를 저장한다(보정은 비동기라 시점이 늦다) — 수동 분석과 동일.
            serviceScope.launch {
                runCatching { recordRepository.saveDetection(result, RecordSource.BACKGROUND) }
            }

            escalateToAiIfConsented(result, pkg, text)
            return
        }

        // 여기까지 왔다는 건 키워드로는 위험이 안 잡혔다는 뜻이다.
        // 그래도 화면에 링크가 있으면 링크 자체를 확인한다 — "긴급하니 눌러보세요" 같은 문구
        // 없이 링크만 툭 던지는 수법이 실제로 흔해서, 키워드 판정만으로는 이런 건이 그대로 지나간다.
        checkLinks(text, pkg, now)
    }

    /**
     * 화면에 있는 링크가 알려진 악성 주소인지 확인하고, 맞으면 알린다.
     *
     * 검사는 비동기라 [onAccessibilityEvent] 를 붙잡지 않는다. 링크가 없으면 아무 일도 하지 않고,
     * 검사에 실패해도 조용히 넘어간다 — 링크 검사 실패가 백그라운드 감지 전체를 멈추면 안 된다.
     */
    private fun checkLinks(text: String, pkg: String, now: Long) {
        if (!linkRiskChecker.isConfigured) return
        val links = LinkExtractor.extract(text)
        if (links.isEmpty()) return

        serviceScope.launch {
            val dangerous = links.firstNotNullOfOrNull { link ->
                runCatching { linkRiskChecker.check(link) }
                    .getOrNull()
                    ?.takeIf { it.verdict == LinkVerdict.DANGEROUS }
            } ?: return@launch

            if (isDuplicateLinkAlert(dangerous.link, now)) return@launch
            lastLinkAlertUrl = dangerous.link.url
            lastLinkAlertAt = now

            alertDangerousLink(dangerous, text, pkg)
        }
    }

    /** 같은 주소는 [SAME_SCREEN_MUTE_MS] 동안 다시 알리지 않는다 — 스크롤·재진입 모두 같은 사건이다. */
    private fun isDuplicateLinkAlert(link: ExtractedLink, now: Long): Boolean =
        link.url == lastLinkAlertUrl && now - lastLinkAlertAt < SAME_SCREEN_MUTE_MS

    /**
     * 악성 링크 1건을 기존 알림·기록 흐름에 실어 보낸다.
     *
     * 키워드가 하나도 안 걸린 건이라 [DetectionResult] 를 여기서 직접 만든다. 차단 목록에
     * 등재된 주소는 그 자체로 확정적인 위험이므로 [RiskLevel.CRITICAL] 로 둔다(키워드 점수처럼
     * 정황을 추정한 값이 아니라 "구글이 악성으로 확인한 주소"라는 사실 판정이다).
     *
     * 결과 화면에서는 원문을 다시 검사해 어떤 링크가 왜 걸렸는지 그대로 보여준다
     * ([com.safelink.app.ui.screens.detection.DetectionViewModel.loadBackgroundResult]).
     */
    private suspend fun alertDangerousLink(risk: LinkRiskResult, text: String, pkg: String) {
        val result = DetectionResult(
            riskLevel = RiskLevel.CRITICAL,
            score = LINK_RISK_SCORE,
            category = risk.threat?.label ?: "악성 링크",
            originalText = text,
            matchedKeywords = emptyList(),
            recommendedInstitutions = emptyList()
        )

        BackgroundDetectionState.update(result, sourceApp = pkg)
        notifier.notifyRisk(result.riskLevel, result.category, risk.link.displayText)
        runCatching { recordRepository.saveDetection(result, RecordSource.BACKGROUND) }
    }

    /**
     * 사용자가 동의한 경우에만 2차 AI 보조 분석을 호출한다.
     *
     * ── 알림을 먼저 띄우고 나서 부른다 ────────────────────────────────
     * AI 응답을 기다렸다 알리면 경고가 1~2초 늦어진다. 백그라운드 감지는 "빨리 알려주는 것"이
     * 존재 이유라 그 지연을 감수할 이유가 없다. 그래서 알림·기록은 온디바이스 판정으로 즉시
     * 처리하고, AI 결과는 도착하는 대로 화면에 실린 결과만 갈아끼운다
     * ([BackgroundDetectionState.refine]) — 사용자가 알림을 눌러 결과를 볼 때쯤이면 대개 도착해 있다.
     *
     * ── 동의가 없으면 아무것도 하지 않는다 ────────────────────────────
     * 백그라운드에서 읽은 텍스트에는 사용자가 인지하지 못한 제3자 대화가 들어 있다.
     * 그래서 기본값은 꺼짐이고, 설정에서 명시적으로 동의한 경우에만 서버로 나간다
     * ([AiConsentStore] KDoc 참고).
     */
    private fun escalateToAiIfConsented(result: DetectionResult, pkg: String, text: String) {
        if (!AiConsentStore.isEnabled(applicationContext)) return
        if (!repository.shouldEscalateToAI(result)) return

        serviceScope.launch {
            val refined = runCatching {
                repository.escalateToAI(
                    result = result,
                    sessionId = UUID.randomUUID().toString(),
                    recentTurns = listOf(text)
                )
            }.getOrNull() ?: return@launch

            // 서버가 실패하면 escalateToAI 가 원본을 그대로 돌려준다 — 그때는 갈아끼울 게 없다.
            if (refined !== result) {
                BackgroundDetectionState.refine(refined, sourceApp = pkg)
            }
        }
    }

    /** 매칭된 키워드 id 집합 — 같은 내용을 다시 본 것인지 판단하는 기준 */
    private fun keywordsOf(result: DetectionResult): Set<String> =
        result.matchedKeywords.mapTo(mutableSetOf()) { it.keywordId }

    /**
     * 같은 사건인지 판정.
     *
     * 키워드 집합이 **정확히** 같을 때만 같은 사건으로 보면, 화면이 그려지는 도중에 읽은
     * 것(일부만 보임)과 다 그려진 뒤 읽은 것(전부 보임)이 서로 다른 사건이 되어 같은 문자로
     * 알림이 두 번 뜬다. 한쪽이 다른 쪽을 포함하기만 하면 같은 화면을 보고 있는 것이므로
     * 같은 사건으로 취급한다.
     */
    private fun isSameEvent(pkg: String, keywords: Set<String>): Boolean {
        if (pkg != lastAlertPkg) return false
        val last = lastAlertKeywords ?: return false
        if (keywords.isEmpty() || last.isEmpty()) return keywords == last
        return keywords.containsAll(last) || last.containsAll(keywords)
    }

    /**
     * 같은 내용을 반복해서 알리지 않도록 거르는 규칙.
     *
     *   - 같은 화면에서 스크롤만 하는 경우: 같은 키워드 집합이면 [SAME_SCREEN_MUTE_MS] 동안 생략
     *   - 대화방을 나갔다가 다시 들어온 경우(창 id가 바뀜): 다시 알린다.
     *     다만 목록 → 대화방처럼 몇 초 안에 이어지는 이동은 한 번의 확인 행동이므로
     *     [SCREEN_CHANGE_MUTE_MS] 안에서는 생략한다.
     *
     * 다른 앱에서 같은 문구가 오면(문자·카톡 각각) 별개 사건이므로 서명에 패키지를 포함해 따로 알린다.
     */
    private fun isDuplicateAlert(
        pkg: String,
        result: DetectionResult,
        windowId: Int,
        now: Long
    ): Boolean {
        if (!isSameEvent(pkg, keywordsOf(result))) return false
        val elapsed = now - lastAlertAt
        return if (windowId == lastAlertWindowId) {
            elapsed < SAME_SCREEN_MUTE_MS
        } else {
            elapsed < SCREEN_CHANGE_MUTE_MS
        }
    }

    /**
     * 현재 활성 창의 노드 트리를 순회하며 보이는 텍스트를 모은다.
     * 대화 앱 화면의 말풍선 텍스트가 이 경로로 수집된다.
     */
    private fun extractVisibleText(pkg: String): String? {
        val sb = StringBuilder()

        // 보통은 활성 창 하나면 충분하다 (카카오톡·문자가 이 경우).
        val active = rootInActiveWindow
        if (active != null && active.packageName?.toString() == pkg) {
            collectText(active, sb)
        }

        // 대화 화면이 여러 창으로 쪼개진 앱에서는 활성 창에 입력창만 들어 있고 말풍선은
        // 다른 창에 남는다. 같은 앱의 나머지 창도 훑어서 빠진 내용을 채운다.
        // (flagRetrieveInteractiveWindows 가 있어야 windows 가 채워진다)
        if (sb.length < MIN_MEANINGFUL_LENGTH) {
            for (window in windows.orEmpty()) {
                val root = window.root ?: continue
                if (root.packageName?.toString() != pkg) continue
                if (root == active) continue
                collectText(root, sb)
                if (sb.length >= MAX_CHARS) break
            }
        }

        return sb.toString().trim().takeIf { it.isNotEmpty() }
    }

    private fun collectText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        node ?: return
        if (sb.length >= MAX_CHARS) return

        val text = node.text?.toString()?.takeIf { it.isNotBlank() }
        if (text != null) {
            sb.append(text).append('\n')
        } else {
            // 커스텀 렌더링을 쓰는 앱(인스타그램 DM 등)은 말풍선 내용이 text 대신
            // contentDescription 에만 담기는 경우가 있다. 다만 "좋아요", "프로필 사진" 같은
            // 버튼 라벨도 같은 자리에 오므로, 문장 길이 이상일 때만 대화 내용으로 본다.
            node.contentDescription?.toString()
                ?.takeIf { it.isNotBlank() && it.length >= MIN_DESCRIPTION_LENGTH }
                ?.let { sb.append(it).append('\n') }
        }

        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb)
        }
    }

    override fun onDestroy() {
        recheckHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onInterrupt() {
        // 서비스 중단 시 상태 초기화 (다음 세션에서 이전 텍스트·알림 이력이 남지 않도록)
        lastText = ""
        lastAnalyzedAt = 0L
        lastAlertPkg = null
        lastAlertKeywords = null
        lastAlertAt = 0L
        lastAlertWindowId = -1
    }

    companion object {
        /**
         * 감지 대상 패키지.
         *
         * 접근성 서비스 설정(accessibility_service_config.xml)에 packageNames 제한을 두지 않아
         * 서비스는 모든 앱의 이벤트를 받는다. 실제 분석 대상은 이 목록으로만 걸러내므로,
         * 대상을 넓히려면 패키지명만 추가하면 된다.
         *
         * 문자(SMS)는 기기 제조사별로 기본 앱이 달라 주요 앱을 함께 등록한다.
         * 설치돼 있지 않은 패키지는 이벤트가 오지 않으므로 그냥 무시된다.
         */
        /**
         * 악성 링크 1건이 확인됐을 때 부여하는 점수.
         * 키워드 점수와 달리 추정이 아니라 확인된 사실이므로 CRITICAL 구간(66~100) 안에서 높게 둔다.
         */
        private const val LINK_RISK_SCORE = 90

        /**
         * 활성 창에서 이만큼도 못 읽었으면 대화 내용이 다른 창에 있다고 보고 나머지 창까지 훑는다.
         * 입력창·툴바만 잡혔을 때가 대략 이 길이 아래다(예: "메시지 입력 / 이모티콘 / 음성메시지").
         */
        private const val MIN_MEANINGFUL_LENGTH = 120

        /**
         * 화면 전환 후 재확인까지 기다리는 시간. 대화 목록이 그려질 정도로는 충분하고,
         * 사용자가 "느리다"고 느끼지 않을 만큼은 짧아야 해서 이 정도로 뒀다.
         */
        private const val RECHECK_DELAY_MS = 700L

        private val MONITORED_PACKAGES = setOf(
            "com.kakao.talk",                    // 카카오톡
            "com.samsung.android.messaging",     // 삼성 메시지(갤럭시 기본 문자)
            "com.google.android.apps.messaging", // Google 메시지(픽셀·다수 기기 기본 문자)
            "com.instagram.android",             // 인스타그램 DM
            "com.discord",                       // 디스코드
            "org.telegram.messenger",            // 텔레그램 - 투자사기·리딩방 유입 경로
            "com.nhn.android.band",              // 네이버 밴드 - 중장년층 사용률이 높아 투자·부업사기 유입
            "jp.naver.line.android",             // 라인 - 로맨스스캠
            "com.facebook.orca",                 // 페이스북 메신저 - 로맨스스캠
            "com.tencent.mm",                    // 위챗 - 해외 기반 로맨스스캠
        )

        /** 연속 이벤트 디바운스 간격 */
        private const val MIN_INTERVAL_MS = 1500L

        /** 같은 화면(대화방)에서 스크롤 등으로 같은 내용이 반복 감지될 때 알림을 쉬는 시간 */
        private const val SAME_SCREEN_MUTE_MS = 30 * 60 * 1000L

        /**
         * 화면이 바뀐 뒤(대화방 재진입 등) 같은 내용이 다시 잡힐 때 알림을 쉬는 시간.
         * 목록 → 대화방처럼 몇 초 안에 이어지는 이동을 한 번으로 묶기 위한 최소 간격.
         */
        private const val SCREEN_CHANGE_MUTE_MS = 60 * 1000L

        /** 노드 순회로 모을 최대 글자 수(성능 보호) */
        private const val MAX_CHARS = 5000

        /**
         * contentDescription 을 대화 내용으로 인정할 최소 길이.
         * 이보다 짧으면 "좋아요", "전송" 같은 버튼 라벨일 가능성이 높다.
         */
        private const val MIN_DESCRIPTION_LENGTH = 10
    }
}
