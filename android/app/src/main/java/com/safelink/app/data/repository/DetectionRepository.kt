package com.safelink.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.KeywordData
import dagger.hilt.android.qualifiers.ApplicationContext
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
}
