package com.safelink.app.background

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.repository.DetectionRepository
import com.safelink.app.notification.RiskNotifier

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
 *   - 노드 순회 길이를 [MAX_CHARS] 로 제한해 과도한 처리를 막는다.
 *
 * ※ 감지 대상 패키지([MONITORED_PACKAGES])와 "감지 후 어떤 화면으로 연결할지" 기준은
 *   기능확장 담당(김선한)과 협의해 확정한다. 라우팅 매핑은 [RiskNotifier.routeFor] 한 곳에 모아둠.
 */
class MessageDetectionService : AccessibilityService() {

    private val repository by lazy { DetectionRepository(applicationContext) }
    private val notifier by lazy { RiskNotifier(applicationContext) }

    private var lastText: String = ""
    private var lastAnalyzedAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in MONITORED_PACKAGES) return

        // 디바운스: 같은 화면 이벤트가 연속으로 오므로 일정 간격으로만 분석
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalyzedAt < MIN_INTERVAL_MS) return

        val text = extractVisibleText() ?: return
        if (text.isBlank() || text == lastText) return

        lastText = text
        lastAnalyzedAt = now

        // 기존 온디바이스 엔진 재사용 — 실제 감지/분석은 여기서 일어난다
        val result = repository.analyze(text)

        // 알림 흐름: 경고(WARNING) 이상일 때만 배너 알림
        if (result.riskLevel.ordinal >= RiskLevel.WARNING.ordinal) {
            notifier.notifyRisk(result.riskLevel, result.category)
        }
    }

    /**
     * 현재 활성 창의 노드 트리를 순회하며 보이는 텍스트를 모은다.
     * 대화 앱 화면의 말풍선 텍스트가 이 경로로 수집된다.
     */
    private fun extractVisibleText(): String? {
        val root = rootInActiveWindow ?: return null
        val sb = StringBuilder()
        collectText(root, sb)
        return sb.toString().trim().takeIf { it.isNotEmpty() }
    }

    private fun collectText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        node ?: return
        if (sb.length >= MAX_CHARS) return
        node.text?.toString()?.let { t ->
            if (t.isNotBlank()) sb.append(t).append('\n')
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb)
        }
    }

    override fun onInterrupt() {
        // 서비스 중단 시 상태 초기화 (다음 세션에서 이전 텍스트가 남지 않도록)
        lastText = ""
        lastAnalyzedAt = 0L
    }

    companion object {
        /** 감지 대상 메신저 패키지 (팀 확정 후 확장) */
        private val MONITORED_PACKAGES = setOf(
            "com.kakao.talk", // 카카오톡
            // "com.samsung.android.messaging", // 삼성 메시지
        )

        /** 연속 이벤트 디바운스 간격 */
        private const val MIN_INTERVAL_MS = 1500L

        /** 노드 순회로 모을 최대 글자 수(성능 보호) */
        private const val MAX_CHARS = 5000
    }
}
