package com.safelink.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverter
import com.safelink.app.data.model.RiskLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionRecordDao {

    @Query("SELECT * FROM detection_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DetectionRecordEntity>>

    @Query("SELECT * FROM detection_records WHERE id = :id")
    suspend fun findById(id: String): DetectionRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DetectionRecordEntity)

    /** 가장 최근 백그라운드 기록(시각 이후) — 같은 대화면 새로 만들지 않고 이어서 갱신하기 위해 */
    @Query("SELECT * FROM detection_records WHERE sourceType = 'BACKGROUND' AND timestamp >= :since ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestBackgroundSince(since: Long): DetectionRecordEntity?

    @Query("UPDATE detection_records SET memo = :memo WHERE id = :id")
    suspend fun updateMemo(id: String, memo: String?)

    /** 최종 판정이 바뀌면(AI 보조분석 반영, 위험 링크 확인) 같은 기록을 갱신한다. */
    @Query("UPDATE detection_records SET riskLevel = :riskLevel, score = :score, category = :category, aiSummary = :aiSummary, aiDetectedPattern = :aiDetectedPattern WHERE id = :id")
    suspend fun updateVerdict(id: String, riskLevel: RiskLevel, score: Int, category: String, aiSummary: String?, aiDetectedPattern: String?)

    /** 사용자가 남긴 피드백(맞음/오탐)을 저장한다. 취소하면 null 로 지운다. */
    @Query("UPDATE detection_records SET userFeedback = :feedback WHERE id = :id")
    suspend fun updateFeedback(id: String, feedback: RecordFeedback?)

    /** 링크 검사가 끝나면 같은 기록에 판정을 남긴다 (검사는 저장보다 늦게 끝날 수 있다). */
    @Query("UPDATE detection_records SET linkResultsJson = :linkResultsJson WHERE id = :id")
    suspend fun updateLinkResults(id: String, linkResultsJson: String)

    /** 오늘 백그라운드 감지로 알림이 뜬 건수 — 홈 "오늘의 알림" */
    @Query("SELECT COUNT(*) FROM detection_records WHERE sourceType = 'BACKGROUND' AND timestamp >= :since")
    fun observeBackgroundCountSince(since: Long): Flow<Int>

    /** 오늘 사용자가 직접 실행한 분석 건수(텍스트·스크린샷) — 홈 "정밀 검사" */
    @Query("SELECT COUNT(*) FROM detection_records WHERE sourceType != 'BACKGROUND' AND timestamp >= :since")
    fun observeManualCountSince(since: Long): Flow<Int>

    @Query("DELETE FROM detection_records")
    suspend fun deleteAll()

    @Query("DELETE FROM detection_records WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface DiagnosisRecordDao {

    @Query("SELECT * FROM diagnosis_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DiagnosisRecordEntity>>

    @Query("SELECT * FROM diagnosis_records WHERE id = :id")
    suspend fun findById(id: String): DiagnosisRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DiagnosisRecordEntity)

    @Query("UPDATE diagnosis_records SET memo = :memo WHERE id = :id")
    suspend fun updateMemo(id: String, memo: String?)

    @Query("DELETE FROM diagnosis_records")
    suspend fun deleteAll()

    @Query("DELETE FROM diagnosis_records WHERE id = :id")
    suspend fun deleteById(id: String)
}

/** enum 컬럼 변환기 — 이름 문자열로 저장해 순서 변경에 영향받지 않게 한다. */
class RecordConverters {

    @TypeConverter
    fun riskLevelToString(level: RiskLevel): String = level.name

    @TypeConverter
    fun stringToRiskLevel(value: String): RiskLevel =
        runCatching { RiskLevel.valueOf(value) }.getOrDefault(RiskLevel.SAFE)

    @TypeConverter
    fun sourceToString(source: RecordSource): String = source.name

    @TypeConverter
    fun feedbackToString(feedback: RecordFeedback?): String? = feedback?.name

    @TypeConverter
    fun stringToFeedback(value: String?): RecordFeedback? =
        value?.let { runCatching { RecordFeedback.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun stringToSource(value: String): RecordSource =
        runCatching { RecordSource.valueOf(value) }.getOrDefault(RecordSource.TEXT_INPUT)
}
