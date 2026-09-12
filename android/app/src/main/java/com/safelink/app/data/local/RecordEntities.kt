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
    val memo: String? = null,
    /**
     * AI 보조분석이 반영된 경우의 설명과 수법. 반영되지 않았으면 null.
     * 반영되면 [riskLevel]·[score] 도 AI 보정이 들어간 최종값으로 갱신한다 — 결과 화면과 기록이
     * 같은 판정을 보이게 하기 위해서다(예전에는 결과는 "긴급", 기록은 "경고"로 어긋났다).
     */
    val aiSummary: String? = null,
    val aiDetectedPattern: String? = null,
    /**
     * 검사 당시의 링크 판정(주소·판정만, JSON). 링크가 없거나 검사하지 못했으면 null.
     * 다시 열 때 오프라인 등으로 재검사가 실패해도 당시 판정이 남도록 저장한다([com.safelink.app.data.link.LinkResultCodec]).
     */
    val linkResultsJson: String? = null,
    /**
     * 사용자가 이 판정에 대해 남긴 피드백. 남기지 않았으면 null.
     * 오탐(위험하지 않은데 위험으로 뜬 경우)을 사용자가 알려줄 수 있게 해, 키워드·임계값을 고칠 근거로 쓴다.
     * 기기 안에만 남으며 자동으로 어디로도 전송되지 않는다.
     */
    val userFeedback: RecordFeedback? = null
)

/** 분석 결과에 대한 사용자 피드백 */
enum class RecordFeedback(val label: String) {
    /** 판정이 맞았다 */
    AGREED("도움이 됐어요"),

    /** 위험하지 않은데 위험으로 떴다(오탐) */
    FALSE_POSITIVE("위험하지 않았어요")
}

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
