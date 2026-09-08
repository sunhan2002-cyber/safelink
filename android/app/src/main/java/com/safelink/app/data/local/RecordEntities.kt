package com.safelink.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.safelink.app.data.model.RiskLevel

/**
 * 대화 분석 기록 (Design.md 3.1 DetectionRecord 확장).
 *
 * ── 원문 저장에 대해 ─────────────────────────────────────────────
 * Design.md 초안은 "원문 미저장"이었으나, 기록 화면에서 "상세 보기"로 판정 근거를 다시
 * 확인하려면 원문이 필요해 [originalText]를 함께 보관하도록 변경했다(팀 논의 결과).
 *   - 저장 위치는 **기기 내 앱 전용 DB뿐**이다. 서버로 전송되는 경로는 존재하지 않으며
 *     (AndroidManifest의 allowBackup=false 로 클라우드 백업에서도 제외),
 *   - 설정 > "데이터 모두 삭제"로 사용자가 언제든 전부 지울 수 있다.
 *
 * @param sourceType 입력 경로 — 목록에서 "어디서 감지된 기록인지" 구분에 사용
 */
@Entity(tableName = "detection_records")
data class DetectionRecordEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val riskLevel: RiskLevel,
    val score: Int,
    val category: String,
    val sourceType: RecordSource,
    val originalText: String,
    /** 감지된 위험 요소(중분류명) — 쉼표 구분. 목록 요약 문구 생성에 사용 */
    val matchedSubcategories: String,
    val matchedKeywordCount: Int,
    val memo: String? = null
)

/** 자가진단 기록 (Design.md 3.1 DiagnosisRecord) */
@Entity(tableName = "diagnosis_records")
data class DiagnosisRecordEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val riskLevel: RiskLevel,
    val score: Int,
    val checkedCount: Int,
    /** 체크된 항목의 결과 문구 — 줄바꿈 구분 */
    val reasons: String,
    val memo: String? = null
)

/** 분석 기록의 입력 경로 */
enum class RecordSource(val label: String) {
    TEXT_INPUT("텍스트 입력"),
    SCREENSHOT("스크린샷 분석"),
    BACKGROUND("백그라운드 감지")
}
