package com.safelink.app.data.repository

import com.safelink.app.data.link.LinkExtractor

/**
 * 결과 화면에서 "위험 표현이 나온 메시지"만 먼저 보여주기 위해, 원문을 줄 단위로 나누고
 * 걸린 표현이나 링크가 들어 있는 줄만 고른다.
 *
 * 줄 위치는 원문 기준 문자 위치(끝은 미포함)로 돌려준다 — 매칭 구간(startIndex/endIndex)이 원문 기준이라
 * 그대로 겹쳐 칠할 수 있어야 한다.
 */
object RiskyLines {

    data class Line(val start: Int, val end: Int)

    fun lines(text: String): List<Line> {
        val result = mutableListOf<Line>()
        var start = 0
        while (start <= text.length) {
            val nl = text.indexOf('\n', start).let { if (it < 0) text.length else it }
            if (text.substring(start, nl).isNotBlank()) result += Line(start, nl)
            start = nl + 1
        }
        return result
    }

    /** 매칭 구간(원문 기준 [start, end))과 겹치거나 링크가 들어 있는 줄. */
    fun select(text: String, matchRanges: List<Pair<Int, Int>>): List<Line> =
        lines(text).filter { line ->
            matchRanges.any { (s, e) -> s < line.end && e > line.start } ||
                LinkExtractor.extract(text.substring(line.start, line.end)).isNotEmpty()
        }
}
