package com.safelink.app.data.repository

import android.content.Context
import com.safelink.app.data.local.DetectionRecordEntity
import com.safelink.app.data.local.DiagnosisRecordEntity
import com.safelink.app.data.local.RecordSource
import com.safelink.app.data.local.SafeLinkDatabase
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.RiskLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    val sourceLabel: String? = null
)

/**
 * 검사 기록 저장·조회 (Task 7.1).
 *
 * 저장은 기기 내 Room DB 한 곳에서만 이뤄지며, 사용자가 설정에서 [deleteAll]로 전부 지울 수 있다.
 */
class RecordRepository(context: Context) {

    private val db = SafeLinkDatabase.get(context)
    private val detectionDao = db.detectionRecordDao()
    private val diagnosisDao = db.diagnosisRecordDao()

    /** 두 종류의 기록을 최신순으로 합친 목록 */
    fun observeRecords(): Flow<List<RecordItem>> =
        combine(detectionDao.observeAll(), diagnosisDao.observeAll()) { detections, diagnoses ->
            (detections.map { it.toItem() } + diagnoses.map { it.toItem() })
                .sortedByDescending { it.timestamp }
        }

    suspend fun findById(id: String, type: RecordType): RecordItem? = when (type) {
        RecordType.DETECTION -> detectionDao.findById(id)?.toItem()
        RecordType.DIAGNOSIS -> diagnosisDao.findById(id)?.toItem()
    }

    /** id만 알고 종류를 모르는 경우(메모 화면 등)에 사용 */
    suspend fun findById(id: String): RecordItem? =
        detectionDao.findById(id)?.toItem() ?: diagnosisDao.findById(id)?.toItem()

    /** 대화 분석 결과 저장. [source]로 텍스트/스크린샷/백그라운드 경로를 구분한다. */
    suspend fun saveDetection(result: DetectionResult, source: RecordSource): String {
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
                matchedKeywordCount = result.matchedKeywords.size
            )
        )
        return id
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

    suspend fun updateMemo(id: String, memo: String?) {
        // 어느 테이블의 기록인지 모르므로 양쪽 모두 시도한다(존재하는 쪽만 갱신됨).
        detectionDao.updateMemo(id, memo)
        diagnosisDao.updateMemo(id, memo)
    }

    /** 설정 > 데이터 모두 삭제 */
    suspend fun deleteAll() {
        detectionDao.deleteAll()
        diagnosisDao.deleteAll()
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
        sourceLabel = sourceType.label
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
