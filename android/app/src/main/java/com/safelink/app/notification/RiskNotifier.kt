package com.safelink.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.safelink.app.MainActivity
import com.safelink.app.R
import com.safelink.app.data.model.RiskLevel

/**
 * 위험 감지 알림 발송 — **구조 스켈레톤**
 *
 * 백그라운드 감지([com.safelink.app.background.MessageDetectionService]) 또는 분석 완료 시
 * 위험도별 배너 알림을 띄운다. 알림/감지 흐름이 이 클래스로 모인다.
 *
 * 설계 기준:
 *   - 위험도별 우선순위 (Design.md 5.3): CAUTION 무음 / WARNING 진동 / CRITICAL 진동+음
 *   - 중립적 문구 (Design.md 7장 + 김우영 문구 가이드): 위험·폭력·상담 등 민감 단어 배제
 *     → 앱 사용 사실이 노출돼도 안전하도록
 *   - 알림 탭 시 위험도별 대응 가이드/긴급 화면으로 딥링크  [TODO: Task 6.15]
 *
 * 주의: Android 13+ 는 POST_NOTIFICATIONS 런타임 권한이 있어야 실제로 표시된다
 *       (Manifest 선언 완료, 권한 요청 UI 는 TODO).
 */
class RiskNotifier(private val context: Context) {

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SafeLink 알림", // TODO: 설정에서 사용자가 바꾼 중립 문구 사용 (neutral_notif_title)
            NotificationManager.IMPORTANCE_HIGH
        )
        manager().createNotificationChannel(channel)
    }

    /** 위험도별 배너 알림 발송. */
    fun notifyRisk(level: RiskLevel, category: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // TODO: 위험도별 딥링크 — CAUTION/WARNING→대응 가이드, CRITICAL→긴급 화면 (Task 6.15)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher) // TODO: 전용 상태바 아이콘
            .setContentTitle(neutralTitle(level))
            .setContentText("확인이 필요한 표현이 감지되었습니다. 내용을 확인해 보세요.")
            .setPriority(priorityOf(level))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        // POST_NOTIFICATIONS 권한 없으면 조용히 무시 (권한 요청 UI 는 TODO)
        runCatching { manager().notify(NOTIF_ID, notification) }
    }

    private fun manager() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** 민감 단어를 뺀 중립 알림 제목. */
    private fun neutralTitle(level: RiskLevel): String = when (level) {
        RiskLevel.CRITICAL -> "지금 확인이 필요해요"
        RiskLevel.WARNING -> "한 번 확인해 보세요"
        else -> "새로운 소식이 있어요"
    }

    private fun priorityOf(level: RiskLevel): Int = when (level) {
        RiskLevel.CRITICAL -> NotificationCompat.PRIORITY_HIGH
        RiskLevel.WARNING -> NotificationCompat.PRIORITY_DEFAULT
        else -> NotificationCompat.PRIORITY_LOW
    }

    companion object {
        const val CHANNEL_ID = "safelink_alert"
        private const val NOTIF_ID = 1001
    }
}
