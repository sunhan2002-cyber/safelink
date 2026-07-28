package com.safelink.app.ui.screens.detection

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.safelink.app.data.model.DetectionResult
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

    var result by mutableStateOf<DetectionResult?>(null)
        private set

    private val repository: DetectionRepository by lazy { DetectionRepository(getApplication()) }

    /** 원문 텍스트를 분석해 result 에 반영한다. Analyzing 화면 진입 전에 호출. */
    fun analyze() {
        result = repository.analyze(originalText)
    }
}
