package com.safelink.app.background

/**
 * 데이터를 모두 삭제하면 백그라운드 감지의 "이미 알린 대화" 기억도 함께 지운다.
 *
 * [MessageDetectionService]는 같은 알림을 반복하지 않으려고 직전 화면 텍스트·알린 문구·AI 전송 이력을 메모리에 들고 있다.
 * 기록을 지워도 이 기억이 남아서, 같은 대화방의 같은 대화로 돌아가면 알림이 다시 뜨지 않았다(시연 중 확인).
 * 삭제할 때 번호를 올리고, 서비스는 다음 분석 전에 번호가 바뀐 것을 보면 기억을 비운다.
 * 서비스와 앱 화면은 같은 프로세스에서 돌아서 메모리 값 하나로 충분하다.
 */
object AlertHistoryReset {

    @Volatile
    var generation: Long = 0L
        private set

    @Synchronized
    fun request() {
        generation++
    }
}
