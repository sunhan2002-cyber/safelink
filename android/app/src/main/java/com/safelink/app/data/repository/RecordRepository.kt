package com.safelink.app.data.repository

import android.content.Context
import com.safelink.app.background.AlertHistoryReset
import com.safelink.app.background.BackgroundDetectionState
import com.safelink.app.data.link.LinkResultCodec
import com.safelink.app.data.link.LinkRiskResult
import com.safelink.app.data.local.DetectionRecordEntity
import com.safelink.app.data.local.DiagnosisRecordEntity
import com.safelink.app.data.local.RecordFeedback
import com.safelink.app.data.local.RecordSource
import com.safelink.app.data.local.SafeLinkDatabase
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.RiskLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.UUID

/** 기록 종류 — 목록에서 분석/진단을 구분한다. */
enum class RecordType { DETECTION, DIAGNOSIS }

/**
 * 기록 화면이 쓰는 통합 모델. 대화 분석·자가진단 두 테이블을 시간순으로 합쳐서 노출한다.
 *
 * @param originalText 대화 분석 기록의 원문(자가진단은 null) — 상세 보기에서 근거를 다시 볼 때 사용
 */
data class RecordItem(
    val id: String,
    val type: RecordType,
    val timestamp: Long,
    val riskLevel: RiskLevel,
    val score: Int,
    val title: String,
    val summary: String,
    val memo: String?,
    val originalText: String? = null,
    /** 대화 분석 기록의 입력 경로 라벨(자가진단은 null) */
    val sourceLabel: String? = null,
    /** AI 보조분석이 반영된 기록이면 설명·수법 (상세 보기에서 복원용) */
    val aiSummary: String? = null,
    val aiDetectedPattern: String? = null,
    /** 대화 분석 기록의 위험 유형(자가진단은 빈 값) — 상세 보기에서 저장 당시 판정 복원용 */
    val category: String = "",
    /** 검사 당시 링크 판정(JSON). 목록에서는 쓰지 않으므로 상세 보기에서만 [LinkResultCodec.decode] 한다. */
    val linkResultsJson: String? = null,
    /** 사용자가 남긴 피드백(맞음/오탐). 남기지 않았으면 null */
    val feedback: RecordFeedback? = null
)

/**
 * 검사 기록 저장·조회 (Task 7.1).
 *
 * 저장은 기기 내 Room DB 한 곳에서만 이뤄지며, 사용자가 설정에서 [deleteAll]로 전부 지울 수 있다.
 */
class RecordRepository(context: Context) {

    private companion object {
        /** 이 시간 안에 같은 대화가 다시 감지되면 기록을 이어서 갱신한다 */
        const val MERGE_WINDOW_MS = 10 * 60 * 1000L

        /** 감지가 몇 초 사이에 겹쳐 들어와도 "기존 기록 찾기 → 저장"이 한 번에 하나씩 되도록 */
        val backgroundSaveLock = Mutex()
    }

    private val db = SafeLinkDatabase.get(context)
    private val detectionDao = db.detectionRecordDao()
    private val diagnosisDao = db.diagnosisRecordDao()

    /** 두 종류의 기록을 최신순으로 합친 목록 */
    fun observeRecords(): Flow<List<RecordItem>> =
        combine(detectionDao.observeAll(), diagnosisDao.observeAll()) { detections, diagnoses ->
            (detections.map { it.toItem() } + diagnoses.map { it.toItem() })
                .sortedByDescending { it.timestamp }
        }

    /**
     * 홈 대시보드 카운터 (오늘 0시 기준).
     *
     * - [observeTodayBackgroundCount] : 백그라운드 감지로 알림이 뜬 건수 → "오늘의 알림"
     * - [observeTodayManualCount]     : 사용자가 직접 실행한 분석 건수 → "정밀 검사"
     *
     * DB에서 세므로 앱을 껐다 켜도 값이 유지되고, 날짜가 바뀌면 자연히 0부터 다시 센다.
     */
    fun observeTodayBackgroundCount(): Flow<Int> =
        detectionDao.observeBackgroundCountSince(startOfToday())

    fun observeTodayManualCount(): Flow<Int> =
        detectionDao.observeManualCountSince(startOfToday())

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    suspend fun findById(id: String, type: RecordType): RecordItem? = when (type) {
        RecordType.DETECTION -> detectionDao.findById(id)?.toItem()
        RecordType.DIAGNOSIS -> diagnosisDao.findById(id)?.toItem()
    }

    /** id만 알고 종류를 모르는 경우(메모 화면 등)에 사용 */
    suspend fun findById(id: String): RecordItem? =
        detectionDao.findById(id)?.toItem() ?: diagnosisDao.findById(id)?.toItem()

    /**
     * 대화 분석 결과 저장. [source]로 텍스트/스크린샷/백그라운드 경로를 구분한다.
     * @param linkResults 저장 시점에 이미 끝난 링크 검사 결과(백그라운드 악성 링크 알림). 나중에 끝나면 [updateLinkResults].
     */
    suspend fun saveDetection(
        result: DetectionResult,
        source: RecordSource,
        linkResults: List<LinkRiskResult> = emptyList()
    ): String {
        val id = UUID.randomUUID().toString()
        detectionDao.insert(
            DetectionRecordEntity(
                id = id,
                timestamp = System.currentTimeMillis(),
                riskLevel = result.riskLevel,
                score = result.score,
                category = result.category,
                sourceType = source,
                originalText = result.originalText,
                matchedSubcategories = result.matchedKeywords
                    .map { it.subcategoryName }
                    .distinct()
                    .joinToString(","),
                // 같은 표현이 여러 규칙에 겹쳐 잡히므로 고유 키워드 기준으로 센다
                // (예: "300만원만 보내"와 "300만원만"은 한 건이다)
                matchedKeywordCount = result.matchedKeywords.distinctBy { it.keywordId }.size,
                linkResultsJson = LinkResultCodec.encode(linkResults)
            )
        )
        return id
    }

    /**
     * 백그라운드 감지 저장 — 몇 분 안에 같은 대화가 다시 감지되면 새 기록을 만들지 않고 기존 기록을 이어서 갱신한다.
     *
     * 메시지가 하나 올 때마다 기록이 새로 쌓여 기록 탭이 같은 대화로 채워지던 문제([ConversationMerge] 참고).
     * - 새 판정 점수가 같거나 높으면: 원문·판정·시각을 새 것으로 바꾼다(메모·피드백은 유지, AI 설명은 새 판정에 맞지 않아 비운다).
     * - 새 판정 점수가 낮으면(스크롤로 위험 문장이 화면 밖으로 나간 경우 등): 더 위험했던 기존 기록을 그대로 둔다.
     * 어느 경우든 같은 기록 id 를 돌려줘 알림을 누르면 그 기록이 열린다.
     */
    suspend fun saveBackgroundDetection(result: DetectionResult, linkResults: List<LinkRiskResult> = emptyList()): String =
        backgroundSaveLock.withLock { mergeOrInsertBackground(result, linkResults) }

    private suspend fun mergeOrInsertBackground(result: DetectionResult, linkResults: List<LinkRiskResult>): String {
        val now = System.currentTimeMillis()
        val latest = detectionDao.latestBackgroundSince(now - MERGE_WINDOW_MS)
        if (latest == null || !ConversationMerge.isSameConversation(latest.originalText, result.originalText)) {
            return saveDetection(result, RecordSource.BACKGROUND, linkResults)
        }
        if (result.score < latest.score) return latest.id
        detectionDao.insert(
            latest.copy(
                timestamp = now,
                riskLevel = result.riskLevel,
                score = result.score,
                category = result.category,
                originalText = result.originalText,
                matchedSubcategories = result.matchedKeywords.map { it.subcategoryName }.distinct().joinToString(","),
                matchedKeywordCount = result.matchedKeywords.distinctBy { it.keywordId }.size,
                aiSummary = null,
                aiDetectedPattern = null,
                linkResultsJson = LinkResultCodec.encode(linkResults) ?: latest.linkResultsJson
            )
        )
        return latest.id
    }

    /** 링크 검사 판정을 기록에 남긴다. 남길 판정이 없으면(전부 검사 실패) 기존 값을 건드리지 않는다. */
    suspend fun updateLinkResults(id: String, results: List<LinkRiskResult>) {
        val json = LinkResultCodec.encode(results) ?: return
        detectionDao.updateLinkResults(id, json)
    }

    /** 자가진단 결과 저장 */
    suspend fun saveDiagnosis(
        score: Int,
        level: RiskLevel,
        reasons: List<String>,
        checkedCount: Int
    ): String {
        val id = UUID.randomUUID().toString()
        diagnosisDao.insert(
            DiagnosisRecordEntity(
                id = id,
                timestamp = System.currentTimeMillis(),
                riskLevel = level,
                score = score,
                checkedCount = checkedCount,
                reasons = reasons.joinToString("\n")
            )
        )
        return id
    }

    /**
     * 최종 판정으로 기록을 갱신한다 — AI 보조분석이 반영됐거나 위험 링크가 확인돼 판정이 바뀐 경우.
     * 온디바이스 결과를 먼저 저장하고, 나중에 도착한 결과를 이 함수로 같은 기록에 맞춘다.
     */
    suspend fun updateVerdict(id: String, verdict: DetectionResult) {
        detectionDao.updateVerdict(
            id = id,
            riskLevel = verdict.riskLevel,
            score = verdict.score,
            category = verdict.category,
            aiSummary = verdict.aiSummary,
            aiDetectedPattern = verdict.aiDetectedPattern
        )
    }

    /**
     * 사용자가 결과 화면에서 남긴 피드백을 기록에 저장한다.
     * 오탐 신고가 쌓이면 어떤 키워드·점수 구간에서 헛짚는지 근거로 쓸 수 있다(기기 안에만 저장).
     */
    suspend fun updateFeedback(id: String, feedback: RecordFeedback?) {
        detectionDao.updateFeedback(id, feedback)
    }

    suspend fun updateMemo(id: String, memo: String?) {
        // 어느 테이블의 기록인지 모르므로 양쪽 모두 시도한다(존재하는 쪽만 갱신됨).
        detectionDao.updateMemo(id, memo)
        diagnosisDao.updateMemo(id, memo)
    }

    /** 설정 > 데이터 모두 삭제 */
    suspend fun deleteAll() {
        detectionDao.deleteAll()
        diagnosisDao.deleteAll()
        // 지운 뒤에는 같은 대화방의 같은 대화도 처음 보는 것처럼 다시 알리고, 홈도 감지 전 기본 화면으로 되돌린다
        AlertHistoryReset.request()
        BackgroundDetectionState.clear()
    }

    /** 기록 한 건 삭제 — 어느 테이블인지 모르므로 양쪽에 시도한다(존재하는 쪽만 삭제됨). */
    suspend fun delete(id: String) {
        detectionDao.deleteById(id)
        diagnosisDao.deleteById(id)
    }
}

private fun DetectionRecordEntity.toItem(): RecordItem {
    val subcategories = matchedSubcategories.split(",").filter { it.isNotBlank() }
    return RecordItem(
        id = id,
        type = RecordType.DETECTION,
        timestamp = timestamp,
        riskLevel = riskLevel,
        score = score,
        title = if (category.isBlank()) "${sourceType.label} 결과" else "${sourceType.label} · $category",
        summary = when {
            subcategories.isEmpty() -> "위험한 표현이 감지되지 않았습니다."
            subcategories.size <= 2 -> "${subcategories.joinToString(", ")} 관련 표현이 감지되었습니다."
            else -> "${subcategories.take(2).joinToString(", ")} 등 ${subcategories.size}개 위험 요소가 감지되었습니다."
        },
        memo = memo,
        originalText = originalText,
        sourceLabel = sourceType.label,
        aiSummary = aiSummary,
        aiDetectedPattern = aiDetectedPattern,
        category = category,
        linkResultsJson = linkResultsJson,
        feedback = userFeedback
    )
}

private fun DiagnosisRecordEntity.toItem(): RecordItem {
    val reasonList = reasons.split("\n").filter { it.isNotBlank() }
    return RecordItem(
        id = id,
        type = RecordType.DIAGNOSIS,
        timestamp = timestamp,
        riskLevel = riskLevel,
        score = score,
        title = "자가진단 결과",
        summary = if (reasonList.isEmpty()) {
            "선택한 항목이 없습니다."
        } else {
            "${checkedCount}개 항목 선택 · ${reasonList.first()}"
        },
        memo = memo
    )
}
