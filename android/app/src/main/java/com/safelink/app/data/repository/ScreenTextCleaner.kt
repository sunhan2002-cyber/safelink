package com.safelink.app.data.repository

/**
 * 백그라운드에서 읽은 화면 텍스트에서 메시지가 아닌 화면 요소 줄을 걸러낸다.
 *
 * ── 왜 필요한가 ────────────────────────────────────────────────────────
 * 접근성 트리에는 말풍선뿐 아니라 시각·요일·읽음 숫자, 버튼 라벨("Show attach media screen",
 * "Conversation Icon", "메시지 입력")까지 들어 있다. 실제 문자 앱 기록을 보면 메시지 1줄에 화면 요소가
 * 7줄씩 붙어, 결과 화면이 읽기 어렵고 AI 에 보내는 내용에도 불필요한 줄이 섞였다.
 *
 * ── 거르는 원칙 ────────────────────────────────────────────────────────
 * 진짜 메시지를 지우면 탐지를 놓치므로 **줄 전체가 확실한 화면 요소일 때만** 뺀다.
 * - 줄 전체가 시각·날짜·요일·"방금" 같은 시간 표시
 * - 줄 전체가 1~2자리 숫자나 "99+"(안 읽은 메시지 수)
 * - 줄 전체가 알려진 버튼·안내 라벨과 같음(부분 일치는 보지 않는다 — "사진 보내줘"는 메시지다)
 * - 바로 앞 줄과 똑같은 줄(같은 창을 두 번 훑어 생긴 중복)
 * 전화번호만 있는 줄은 남긴다 — "이 번호로 연락" 류 규칙이 번호를 함께 봐야 한다.
 */
object ScreenTextCleaner {

    private val TIME_OR_DATE = listOf(
        // 스크린샷 맨 위 상태표시줄("SKT 10:24")도 시각 줄로 본다
        Regex("(SKT|KT|LG\\s*U\\+|LGU\\+|U\\+)?\\s*(어제|오늘|[월화수목금토일]요일)?\\s*(오전|오후)?\\s*\\d{1,2}:\\d{2}(\\s*(AM|PM|am|pm))?"),
        Regex("\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일(\\s*\\(?[월화수목금토일](요일)?\\)?)?"),
        Regex("\\d{1,2}월\\s*\\d{1,2}일(\\s*\\(?[월화수목금토일](요일)?\\)?)?"),
        // 상태표시줄 배터리·신호("100%", 인식이 번진 "ll 100%을", "LTE", "5G")
        Regex("[A-Za-z|.\\s]{0,4}\\d{1,3}\\s*%\\s*[을를]?"),
        Regex("(LTE|5G|4G|3G|Wi-?Fi)\\+?", RegexOption.IGNORE_CASE),
        Regex("\\d{4}[.\\-/]\\s*\\d{1,2}[.\\-/]\\s*\\d{1,2}\\.?"),
        Regex("[월화수목금토일]요일"),
        Regex("(Mon|Tue|Wed|Thu|Fri|Sat|Sun)(day|sday|nesday|rsday|urday)?", RegexOption.IGNORE_CASE),
        Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2}(,\\s*\\d{4})?", RegexOption.IGNORE_CASE),
        Regex("(방금|어제|오늘|그저께|Now|Just now|Yesterday|Today)", RegexOption.IGNORE_CASE),
        Regex("\\d{1,2}\\s*(분|시간|일)\\s*전"),
        Regex("\\d{1,2}\\s*(min|mins|h|hr|hrs|d)\\s*(ago)?", RegexOption.IGNORE_CASE),
    )

    // 금액만 답한 메시지("300")는 남기도록 두 자리까지만 본다
    private val UNREAD_COUNT = Regex("\\d{1,2}|\\d{2,3}\\+")

    private val UI_LABEL_PATTERNS = listOf(
        Regex("Show .+ screen", RegexOption.IGNORE_CASE),
        Regex("Texting with .+", RegexOption.IGNORE_CASE),
        // 디스코드: 서버 목록("읽지 않은 메시지, 서버이름"), 접속자 수, 보낸 사람·시각 묶음, 입력창 안내
        Regex("읽지 않은 메시지,\\s*.+"),
        Regex("\\d+명 온라인"),
        Regex(".{1,40},\\s*(오전|오후)\\s*\\d{1,2}:\\d{2}"),
        Regex("#?.{1,40}에(게)? 메시지 보내기"),
        Regex("(읽지 않은 )?.{1,40}\\((채팅|음성|포럼|공지|스테이지) 채널\\)"),
        Regex("(채팅|음성|포럼|공지|스테이지) 채널"),
        Regex("#.{1,40}에 오신 걸 환영합니다!?"),
        Regex(".{1,40}, 멤버 목록"),
        // 인스타그램 DM: 스토리 버튼, 활동 상태, 사진 전송 안내, 입력창 도움말
        Regex(".{1,40}님 스토리 열기"),
        Regex("(최근 활동:?\\s*.{1,20}|현재 활동 중|활동 중)"),
        Regex(".{1,40}님이 (사진|동영상|음성 메시지|게시물|릴스|스티커).{0,20}(보냈습니다|공유했습니다)"),
        Regex("사라지는 메시지를 설정하려면.{0,20}"),
        Regex("/\\S+ .{0,6}사용해보기"),
        Regex("음성 메시지, .{0,20}누르세요"),
        // 이모지·기호만 있는 줄(글자·숫자가 하나도 없음)
        Regex("[^\\p{L}\\p{N}]+"),
        Regex("#.{1,40} 채널의 시작이에요\\.?"),
        Regex(".{1,20}이후로 읽지 않은 메시지가 \\d+개 있어요"),
        Regex("\\d+(초|분|시간|일|주)\\s*전에\\s*읽음|읽음\\s*\\d{1,2}:\\d{2}"),
        // 보낸 사람 아이디("jaegyeom0247", "sunhan04242") — 영문과 숫자·밑줄·점이 섞인 한 단어만. "hello" 같은 영어 메시지는 남긴다
        Regex("(?=.*[A-Za-z])(?=.*[0-9_.])[A-Za-z0-9_.]{3,32}"),
    )

    /** 디스코드 채널 목록은 "과제방 (채팅 채널)" 다음 줄에 이름("과제방")만 한 번 더 온다 */
    private val CHANNEL_ENTRY = Regex("(읽지 않은 )?(.{1,40}) \\((채팅|음성|포럼|공지|스테이지) 채널\\)")

    /** 줄 전체가 이것과 같을 때만 뺀다(대소문자·앞뒤 공백 무시). */
    private val UI_LABELS = setOf(
        // 문자(Google Messages)
        "conversation icon", "text message", "unread", "tap to load preview", "start chat",
        "link previews are on.", "learn more or turn off in settings.", "send sms", "send message",
        "more options", "search", "back", "navigate up", "delivered", "read", "sent", "sending…", "sending...",
        // 카카오톡·공통 한국어 라벨
        "메시지 입력", "메시지를 입력하세요", "메시지 보내기", "전송", "보내기", "검색", "뒤로", "뒤로 가기",
        "메뉴", "더보기", "옵션 더보기", "이모티콘", "첨부", "첨부하기", "카메라", "음성 메시지", "음성메시지",
        "읽음", "안 읽음", "전송됨", "전송 중", "채팅방 서랍", "샵검색", "알림 끄기", "통화하기",
        // 인스타그램 DM
        "메시지...", "메시지…", "좋아요", "답장", "사진", "동영상", "갤러리", "음성 클립", "스티커",
        "message...", "message…", "like", "reply", "gallery", "voice clip", "sticker",
        // 디스코드
        "미디어 키보드 전환", "이모지 키보드 전환", "선물 보내기", "음성 메시지 녹음", "검색하기", "멤버 목록",
        "bottom sheet backdrop", "bottom sheet", "사진 찾아보기", "투표", "스레드", "앱", "파일", "온라인", "오프라인",
        "찾던 사진이 아닌가 보죠? 사진 라이브러리에서 완벽한 사진을 찾아보세요.",
    )

    fun clean(text: String): String {
        val kept = mutableListOf<String>()
        var channelName: String? = null
        for (raw in text.split('\n')) {
            val line = raw.trim()
            if (line == channelName) { channelName = null; continue }
            channelName = CHANNEL_ENTRY.matchEntire(line)?.groupValues?.get(2)
            if (line.isEmpty() || isScreenElement(line)) continue
            if (kept.lastOrNull() == line) continue
            kept += line
        }
        return kept.joinToString("\n")
    }

    fun isScreenElement(line: String): Boolean {
        val t = line.trim()
        if (UNREAD_COUNT.matches(t)) return true
        if (TIME_OR_DATE.any { it.matches(t) }) return true
        if (t.lowercase() in UI_LABELS) return true
        return UI_LABEL_PATTERNS.any { it.matches(t) }
    }
}
