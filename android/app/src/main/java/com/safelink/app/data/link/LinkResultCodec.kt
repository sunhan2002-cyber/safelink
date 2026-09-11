package com.safelink.app.data.link

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 링크 검사 결과를 검사 기록에 저장하고 다시 읽는다.
 *
 * ── 왜 저장하는가 ─────────────────────────────────────────────────────
 * 예전에는 기록을 다시 열 때마다 원문의 링크를 새로 검사했다. 그러면
 * - 오프라인이거나 차단 목록이 준비되지 않은 순간에는 "검사하지 못함"만 남아, 당시 위험 판정이 사라지고
 * - 백그라운드에서 악성 링크로 알렸던 기록도 다시 열면 링크 경고가 보이지 않을 수 있었다.
 * 그래서 검사 당시의 판정을 기록에 남기고, 다시 열 때는 새 검사 결과와 [merge] 한다.
 *
 * 저장하는 건 주소와 판정뿐이다. 기록 DB 는 기기 안에만 있고(원문도 이미 같은 곳에 저장된다) 밖으로 나가지 않는다.
 */
object LinkResultCodec {

    private val gson = Gson()

    /** 저장 형태. Gson 은 Kotlin 널 안정성을 지키지 않으므로 전부 nullable 로 받는다. */
    private data class Stored(
        val url: String? = null,
        val displayText: String? = null,
        val obfuscated: Boolean? = null,
        val verdict: String? = null,
        val threat: String? = null
    )

    /** 검사하지 못한 건([LinkVerdict.UNCHECKED])은 판정이 아니므로 남기지 않는다. 남길 게 없으면 null. */
    fun encode(results: List<LinkRiskResult>): String? {
        val checked = results.filter { it.verdict != LinkVerdict.UNCHECKED }
        if (checked.isEmpty()) return null
        return gson.toJson(
            checked.map { Stored(it.link.url, it.link.displayText, it.link.obfuscated, it.verdict.name, it.threat?.name) }
        )
    }

    /** 저장된 판정을 읽는다. 비었거나 깨진 값은 빈 목록으로 본다 — 기록 열기가 이것 때문에 실패하면 안 된다. */
    fun decode(json: String?): List<LinkRiskResult> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Stored?>>() {}.type
        val stored = runCatching { gson.fromJson<List<Stored?>>(json, type) }.getOrNull() ?: return emptyList()
        return stored.mapNotNull { s ->
            val url = s?.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val verdict = s.verdict?.let { runCatching { LinkVerdict.valueOf(it) }.getOrNull() }
                ?.takeIf { it != LinkVerdict.UNCHECKED }
                ?: return@mapNotNull null
            LinkRiskResult(
                link = ExtractedLink(url = url, displayText = s.displayText ?: url, obfuscated = s.obfuscated ?: false),
                verdict = verdict,
                threat = s.threat?.let { runCatching { LinkThreat.valueOf(it) }.getOrNull() }
            )
        }
    }

    /**
     * 저장된 판정과 새로 검사한 결과를 합친다.
     *
     * - 당시 위험([LinkVerdict.DANGEROUS])이었던 주소는 그대로 위험으로 둔다. 나중에 목록에서 빠졌더라도
     *   이 대화에서 위험한 링크를 받았다는 사실은 바뀌지 않는다.
     * - 새 검사가 실패([LinkVerdict.UNCHECKED])하면 저장된 판정을 쓴다.
     * - 그 밖에는 새 결과를 쓴다(당시 "목록에 없음"이던 주소가 이후 위험으로 등재된 경우 등).
     * - 새 검사에서 빠진 저장 판정(검사 자체가 실패해 목록이 비었을 때 등)도 버리지 않고 뒤에 붙인다.
     */
    fun merge(stored: List<LinkRiskResult>, fresh: List<LinkRiskResult>): List<LinkRiskResult> {
        val storedByUrl = stored.associateBy { it.link.url }
        val merged = fresh.map { f ->
            val s = storedByUrl[f.link.url] ?: return@map f
            if (s.verdict == LinkVerdict.DANGEROUS || f.verdict == LinkVerdict.UNCHECKED) s.copy(link = f.link) else f
        }
        val freshUrls = fresh.mapTo(HashSet()) { it.link.url }
        return merged + stored.filter { it.link.url !in freshUrls }
    }
}
