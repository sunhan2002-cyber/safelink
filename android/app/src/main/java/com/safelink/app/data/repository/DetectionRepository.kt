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
}
