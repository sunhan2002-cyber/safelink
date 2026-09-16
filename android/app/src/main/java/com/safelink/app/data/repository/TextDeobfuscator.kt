package com.safelink.app.data.repository

import com.safelink.app.data.link.LinkExtractor

/**
 * 탐지를 피하려고 글자 사이에 끼워 넣은 기호·보이지 않는 문자를 지운다.
 *
 * "송.금해 주세요", "입*금", "인_증_번_호", 보이지 않는 문자(zero-width)를 끼운 "송\u200B금"처럼 쓰면
 * 규칙이 "송금"을 찾지 못했다. 규칙 엔진은 지운 사본으로 판정하고, 밑줄 위치는 [originalIndex]로 원문에 되돌린다.
 *
 * ── 무엇을 지우는가 ─────────────────────────────────────────────────
 * - 보이지 않는 문자(zero-width space·joiner, soft hyphen, BOM)는 어디에 있든 지운다.
 * - 기호는 **한글 두 글자 사이에 공백 없이 1~3개** 끼어 있을 때만 지운다.
 *   링크("대장방문.com", "택배조회.한국")는 링크 검사와 같은 추출기로 찾아 건드리지 않고, "응. 알겠어"처럼 공백을 둔
 *   보통 문장 부호도 남는다. 물음표·느낌표·물결·말줄임표는 흔한 문장 끝 표시라 지우지 않는다.
 */
internal object TextDeobfuscator {

    class Cleaned(val text: String, val originalIndex: IntArray, val changed: Boolean)

    private val INVISIBLE = setOf('\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF', '\u00AD')

    private val FILLER = Regex("""(?<=[가-힣ㄱ-ㅎㅏ-ㅣ])[.,·•*_\-^'"`/\\|+=#@$%&:;]{1,3}(?=[가-힣ㄱ-ㅎㅏ-ㅣ])""")

    fun clean(text: String): Cleaned {
        val drop = BooleanArray(text.length)
        var changed = false
        text.forEachIndexed { i, c -> if (c in INVISIBLE) { drop[i] = true; changed = true } }
        // 보이지 않는 문자를 먼저 뺀 문자열에서 기호를 찾아야 "송\u200B.금" 같은 겹친 우회도 잡힌다
        val visible = StringBuilder(text.length)
        val visibleIndex = IntArray(text.length)
        var n = 0
        text.forEachIndexed { i, c -> if (!drop[i]) { visible.append(c); visibleIndex[n++] = i } }
        val links = linkRangesOf(visible.toString())
        FILLER.findAll(visible).forEach { m ->
            if (links.any { it.first <= m.range.first && m.range.last <= it.last }) return@forEach
            for (j in m.range) drop[visibleIndex[j]] = true
            changed = true
        }
        if (!changed) return Cleaned(text, IntArray(text.length) { it }, false)

        val sb = StringBuilder(text.length)
        val index = IntArray(text.length)
        var k = 0
        text.forEachIndexed { i, c -> if (!drop[i]) { sb.append(c); index[k++] = i } }
        return Cleaned(sb.toString(), index.copyOf(k), true)
    }

    private fun linkRangesOf(text: String): List<IntRange> {
        var from = 0
        return LinkExtractor.extract(text).mapNotNull { link ->
            val idx = text.indexOf(link.displayText, from).takeIf { it >= 0 } ?: return@mapNotNull null
            from = idx + link.displayText.length
            idx until from
        }
    }
}
