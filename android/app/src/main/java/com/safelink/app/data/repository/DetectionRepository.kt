package com.safelink.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import com.safelink.app.data.remote.AnalyzeApiClient
import com.safelink.app.data.remote.AnalyzeApiService
import com.safelink.app.data.remote.dto.AnalyzeRequestDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * assets/keyword.json, assets/institutions.json을 읽어 [DetectionEngine]을 만들고
 * ViewModel에 [analyze] 진입점만 노출하는 Android 전용 래퍼.
 *
 * 실제 매칭/점수/추천 로직은 Android에 의존하지 않는 [DetectionEngine]에 있음 —
 * 순수 Kotlin 유닛 테스트는 DetectionEngine을 직접 사용.
 */
@Singleton
class DetectionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val engine: DetectionEngine by lazy {
        DetectionEngine(
            keywordData = loadAsset("keyword.json", KeywordData::class.java),
            institutionData = loadAsset("institutions.json", InstitutionData::class.java),
            gson = gson
        )
    }

    /** 2차 AI 보조 분석 서버 클라이언트. 지금은 목 서버(backend/) 기준 기본 주소로 연결됨
     *  — 실제 배포 서버가 생기면 baseUrl만 바꾸면 됨(요청/응답 계약은 그대로). */
    private val apiService: AnalyzeApiService by lazy { AnalyzeApiClient.create() }

    private fun <T> loadAsset(fileName: String, clazz: Class<T>): T {
        val json = context.assets.open(fileName).bufferedReader().use(BufferedReader::readText)
        return gson.fromJson(json, clazz)
    }

    /** 원문 텍스트 1건을 분석해서 [DetectionResult]로 변환. ViewModel에서는 이거 하나만 호출하면 됨. */
    fun analyze(originalText: String): DetectionResult = engine.analyze(originalText)

    /**
     * [analyze] 결과를 가지고 2차 AI API 보조 분석이 필요한지 판단한다. true가 나오면
     * 그 시점에 백엔드 `/analyze`를 호출하고 `context_score_adjustment`를 받아 반영하면 됨
     * (백엔드 자체는 아직 미구현 — 이 함수는 "호출해야 하는가"까지만 책임짐).
     * 조건 근거: 신기훈 4주차 06번 문서 "AI API 진입 조건 확정본".
     */
    fun shouldEscalateToAI(
        result: DetectionResult,
        sessionTurnCount: Int,
        manualReportFlag: Boolean = false,
        newSubcategoryRolloutActive: Boolean = true
    ): Boolean = engine.shouldEscalateToAI(result, sessionTurnCount, manualReportFlag, newSubcategoryRolloutActive)

    /**
     * [shouldEscalateToAI]가 true일 때 실제로 서버를 호출해서 온디바이스 결과를 보정한다.
     * 원문은 [DetectionEngine.maskSensitiveInfo]로 전화번호/URL을 마스킹한 뒤에만 전송하고,
     * 서버 응답의 `context_score_adjustment`만 점수에 반영한다 — 서버가 최종 위험도를
     * 결정하지 않는다는 원칙(CLAUDE.md) 그대로. `recommended_level_override`는 응답에
     * 담겨오지만 3단계/4단계 라벨 혼동 위험이 있어 이번 버전에서는 자동 반영하지 않고
     * 클라이언트(온디바이스) 판단을 그대로 유지한다 — "기본은 null, 클라이언트 판단 존중"
     * 원칙과 동일.
     *
     * 네트워크 실패/타임아웃/서버 오류(503류) 시에는 **온디바이스 결과를 그대로 반환**한다
     * (Design.md 원칙: 서버는 필수 경로가 아니다).
     *
     * @param result 온디바이스 [analyze] 결과
     * @param sessionId 세션(대화방) 식별자
     * @param recentTurns 최근 턴 원문 목록(마스킹 전) — 최근 10턴 이내로 호출부에서 잘라서 넘길 것
     */
    suspend fun escalateToAI(
        result: DetectionResult,
        sessionId: String,
        recentTurns: List<String>
    ): DetectionResult {
        return try {
            val maskedTurns = recentTurns.map { engine.maskSensitiveInfo(it) }
            val maskedText = maskedTurns.lastOrNull() ?: engine.maskSensitiveInfo(result.originalText)
            val request = AnalyzeRequestDto(
                sessionId = sessionId,
                maskedText = maskedText,
                recentTurns = maskedTurns,
                deviceBaseScore = result.score.toDouble(),
                deviceMatchedIds = result.matchedKeywords.map { it.keywordId },
                deviceAppliedComboIds = result.appliedComboIds,
                categoryHint = result.category.ifBlank { null }
            )
            val response = apiService.analyze(request)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                result // 4xx/5xx - 온디바이스 결과 유지
            } else {
                val adjustedScore = (result.score + body.contextScoreAdjustment).toInt().coerceIn(0, 100)
                result.copy(score = adjustedScore, riskLevel = RiskLevel.fromScore(adjustedScore))
            }
        } catch (e: CancellationException) {
            throw e // 구조적 동시성 - 취소는 그대로 전파
        } catch (e: Exception) {
            result // 네트워크 실패/타임아웃 - 온디바이스 결과 그대로 (Design.md 원칙)
        }
    }
}
