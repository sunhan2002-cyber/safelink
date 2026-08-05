package com.safelink.app.background

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.repository.DetectionRepository
import com.safelink.app.notification.RiskNotifier

/**
 * 백그라운드 위험 감지 서비스 — **구조 스켈레톤** (이번 주: 어디에 들어갈지 코드 자리 확보)
 *
 * ── 왜 AccessibilityService 인가 ─────────────────────────────────────────
 * 안드로이드에서 다른 앱(카카오톡·문자 등)의 화면 텍스트를 백그라운드로 읽는 표준·합법 경로는
 * AccessibilityService 다. 사용자가 [설정 > 접근성]에서 SafeLink 를 직접 켜야만 동작하며,
 * 켜기 전에는 실행되지 않는다(개인정보 보호 + Play 정책 준수).
 *
 * ── 감지 → 분석 → 알림 흐름 (각 단계가 붙는 지점) ────────────────────────
 *   1. onAccessibilityEvent  : 감지 대상 메신저의 화면 변화 수신
 *   2. extractVisibleText()  : 화면 노드 트리에서 대화 텍스트 추출        [TODO]
 *   3. DetectionRepository.analyze(text) : 온디바이스 위험 분석          [기존 엔진 재사용]
 *   4. RiskNotifier.notifyRisk()         : 경고 이상이면 배너 알림         [알림 흐름]
 *   5. (알림 탭) → 대응 가이드 / 긴급 화면 딥링크                          [Task 6.15]
 *
 * ── 아직 미구현(다음 단계 TODO) ─────────────────────────────────────────
 *   - 화면 노드 → 텍스트 추출 로직
 *   - 같은 대화 중복 감지 억제(디바운스)·세션 누적 점수
 *   - 배터리/성능 최적화(이벤트 필터링, notificationTimeout)
 *   - 접근성 권한 온보딩 안내 UI (설정으로 유도)
 */
class MessageDetectionService : AccessibilityService() {

    private val repository by lazy { DetectionRepository(applicationContext) }
    private val notifier by lazy { RiskNotifier(applicationContext) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in MONITORED_PACKAGES) return

        val text = extractVisibleText() ?: return
        if (text.isBlank()) return

        // 기존 온디바이스 엔진 재사용 — 실제 감지/분석은 여기서 일어난다
        val result = repository.analyze(text)

        // 알림 흐름: 경고(WARNING) 이상일 때만 배너 알림
        if (result.riskLevel.ordinal >= RiskLevel.WARNING.ordinal) {
            notifier.notifyRisk(result.riskLevel, result.category)
        }
    }

    /**
     * 현재 화면에서 대화 텍스트를 추출한다. (미구현)
     * TODO: rootInActiveWindow 노드 트리를 순회하며 TextView 계열 노드의 text 를 모은다.
     */
    private fun extractVisibleText(): String? {
        // val root = rootInActiveWindow ?: return null
        // return collectText(root)
        return null
    }

    override fun onInterrupt() { /* 서비스 중단 시 처리 — 현재 없음 */ }

    companion object {
        /** 감지 대상 메신저 패키지 (팀 확정 후 확장) */
        private val MONITORED_PACKAGES = setOf(
            "com.kakao.talk", // 카카오톡
            // "com.samsung.android.messaging", // 삼성 메시지
        )
    }
}
