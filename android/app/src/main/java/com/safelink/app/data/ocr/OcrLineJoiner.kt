package com.safelink.app.data.ocr

/**
 * 말풍선 하나(인식 블록 하나) 안에서 폭 때문에 접힌 줄들을 한 메시지로 잇는다.
 *
 * 한국어 메신저는 글자 단위로 줄을 접는다("송금해 주세 / 요", "심 / 각해?"). 그래서 한글·숫자끼리
 * 이어지는 경계는 띄어쓰기 없이 붙이고, 영어 단어처럼 공백으로 접혔을 가능성이 큰 경계만 한 칸 띄운다.
 * 원래 띄어쓰기 자리에서 접힌 경우("엄마 카드 / 사진")는 공백 하나가 사라지지만, 탐지 규칙은
 * 단어 사이 공백 유무를 모두 허용하므로 판정에는 영향이 없다.
 */
object OcrLineJoiner {

    fun join(lines: List<String>): String {
        val sb = StringBuilder()
        lines.map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            if (sb.isNotEmpty() && needsSpace(sb.last(), line.first())) sb.append(' ')
            sb.append(line)
        }
        return sb.toString()
    }

    private fun needsSpace(prev: Char, next: Char): Boolean = !(isGlued(prev) && isGlued(next))

    /** 한글·숫자·문장 부호는 글자 단위로 접히므로 붙인다. */
    private fun isGlued(c: Char): Boolean =
        c in '가'..'힣' || c in 'ㄱ'..'ㅣ' || c.isDigit() || c in ".,?!~)(%'\"-"
}
