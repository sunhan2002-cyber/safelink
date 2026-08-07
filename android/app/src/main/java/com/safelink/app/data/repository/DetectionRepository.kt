package com.safelink.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.safelink.app.data.model.DetectionResult
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
     * 그 시점에 [escalateToAI]로 실제 호출한다. 조건 근거: 신기훈 4주차 06번 문서
     * "AI API 진입 조건 확정본" + 5주차 정리(가스라이팅 반복/장기세션은 온디바이스 점수
     * 콤보로 옮겨서 여기서 제거 — `DetectionEngine.shouldEscalateToAI` KDoc 참고).
     */
    fun shouldEscalateToAI(
        result: DetectionResult,
        manualReportFlag: Boolean = false,
        newSubcategoryRolloutActive: Boolean = true
    ): Boolean = engine.shouldEscalateToAI(result, manualReportFlag, newSubcategoryRolloutActive)

    /**
     * [shouldEscalateToAI]가 true일 때 실제로 서버를 호출해서 온디바이스 결과를 보정한다.
     * 원문은 [DetectionEngine.maskSensitiveInfo]로 전화번호/URL을 마스킹한 뒤에만 전송하고,
     * 병합 규칙 자체는 [DetectionEngine.mergeAiResponse]에 있음(점수/riskLevel/추천기관/
     * AI 요약 문구 — 신기훈 4주차 07번 문서 "AI 응답 반영 범위" 참고). 이 함수는 네트워크
     * 호출과 성공/실패 분기만 책임진다.
     *
     * `recommended_level_override`는 응답에 담겨오지만 3단계/4단계 라벨 혼동 위험이 있어
     * 자동 반영하지 않고 클라이언트(온디바이스) 판단을 그대로 유지한다 — "기본은 null,
     * 클라이언트 판단 존중" 원칙과 동일.
     *
     * **실패 시 온디바이스 결과를 그대로 반환하는 경로 3가지** (Design.md 원칙: 서버는
     * 필수 경로가 아니다 — 무슨 일이 있어도 화면에 아무것도 안 뜨는 상황은 없어야 함):
     *   1. HTTP 응답이 실패(4xx/5xx, 예: 429 호출한도초과·503 서버오류) → `!response.isSuccessful`
     *   2. HTTP는 200인데 응답 바디가 비어있음(파싱 실패 등) → `body == null`
     *   3. 네트워크 자체가 실패(타임아웃·연결거부·DNS 실패 등) → `catch (e: Exception)`
     * 세 경로 전부 동일하게 `result`(원본 온디바이스 결과)를 그대로 반환한다.
     *
     * @param result 온디바이스 [analyze] 결과
     * @param sessionId 세션(대화방) 식별자
     * @param recentTurns 최근 턴 원문 목록(마스킹 전) — 최근 10턴 이내로 호출부에서 잘라서 넘길 것.
     *   **한계**: 지금 유일한 호출부인 [com.safelink.app.ui.screens.detection.DetectionViewModel]은
     *   `listOf(originalText)`(입력 1건)만 넘긴다 — 진짜 다중 턴 세션이 아니라 단일 입력
     *   기준이라는 뜻. 실제 다중 턴 추적이 생기면 그 상태를 그대로 넘기면 됨(이 함수 자체는
     *   턴 개수와 무관하게 동작).
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
                result // 경로 1·2 - 온디바이스 결과 유지
            } else {
                engine.mergeAiResponse(result, body)
            }
        } catch (e: CancellationException) {
            throw e // 구조적 동시성 - 취소는 그대로 전파 (온디바이스 결과 폴백 대상 아님)
        } catch (e: Exception) {
            result // 경로 3 - 네트워크 실패/타임아웃, 온디바이스 결과 그대로
        }
    }
}
