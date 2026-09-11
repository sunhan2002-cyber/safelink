package com.safelink.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.safelink.app.data.model.raw.InstitutionData
import com.safelink.app.data.model.raw.InstitutionEntry
import java.io.BufferedReader

/**
 * assets/institutions.json의 기관 목록만 UI용으로 가볍게 읽어온다.
 * [com.safelink.app.data.repository.DetectionRepository]도 같은 파일을 읽지만 그쪽은
 * 위험도 분석 엔진 구성용(Hilt 싱글톤)이라 목적이 다름 — 지원 탭 화면들은 분석 없이
 * 기관 목록만 필요해서 별도로 가볍게 분리.
 */
object InstitutionCatalog {
    @Volatile
    private var cache: List<InstitutionEntry>? = null

    fun load(context: Context): List<InstitutionEntry> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val json = context.assets.open("institutions.json")
                .bufferedReader()
                .use(BufferedReader::readText)
            val loaded = Gson().fromJson(json, InstitutionData::class.java).institutions
            cache = loaded
            return loaded
        }
    }
}
