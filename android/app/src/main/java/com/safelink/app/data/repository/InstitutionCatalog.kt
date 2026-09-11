package com.safelink.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.InstitutionEntry
import com.safelink.app.data.model.raw.InstitutionPriorityEntry
import java.io.BufferedReader

/**
 * assets/institutions.json을 UI용으로 가볍게 읽어온다(기관 목록 + 위험유형별 우선순위).
 * [com.safelink.app.data.repository.DetectionRepository]도 같은 파일을 읽지만 그쪽은
 * 위험도 분석 엔진 구성용(Hilt 싱글톤)이라 목적이 다름 — 지원 탭 화면들은 분석 없이
 * 데이터만 필요해서 별도로 가볍게 분리.
 */
object InstitutionCatalog {
    @Volatile
    private var cache: InstitutionData? = null

    private fun data(context: Context): InstitutionData {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val json = context.assets.open("institutions.json")
                .bufferedReader()
                .use(BufferedReader::readText)
            return Gson().fromJson(json, InstitutionData::class.java).also { cache = it }
        }
    }

    fun load(context: Context): List<InstitutionEntry> = data(context).institutions

    /** institutions.json risk_type_priority 원본 그대로("기관사칭" 등 7분류 키 -> 순위 목록) */
    fun riskTypePriority(context: Context): Map<String, List<InstitutionPriorityEntry>> =
        data(context).riskTypePriority
}
