package com.safelink.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 앱 잠금 정책 상수 검증 (Task 5.12, Design.md 5.4).
 *
 * 실제 저장·검증 동작은 SharedPreferences(Android)에 의존해 계측 테스트 영역이라,
 * 여기서는 스펙에 못 박힌 임계값이 코드에서 바뀌지 않았는지만 지킨다.
 */
class AppLockPolicyTest {

    @Test
    fun `연속 실패 허용 횟수는 5회`() {
        assertEquals(5, AppLockManager.MAX_FAIL_COUNT)
    }

    @Test
    fun `차단 시간은 30초`() {
        assertEquals(30_000L, AppLockManager.LOCKOUT_MS)
    }

    @Test
    fun `차단 시간은 사용자가 기다릴 수 있는 범위여야 함`() {
        // 너무 짧으면 무차별 입력을 못 막고, 너무 길면 정작 본인이 못 들어온다
        assertTrue(AppLockManager.LOCKOUT_MS >= 10_000L)
        assertTrue(AppLockManager.LOCKOUT_MS <= 60_000L)
    }
}
