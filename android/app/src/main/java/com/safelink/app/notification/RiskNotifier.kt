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
import com.safelink.app.settings.NotificationTextStore
import com.safelink.app.ui.navigation.Screen

class RiskNotifier(private val context: Context) {

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SafeLink 알림",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager().createNotificationChannel(channel)
    }

    /**
     * @param recordId 이 감지가 저장된 기록 id. 있으면 알림을 눌렀을 때 그 기록의 분석 결과 화면이 열린다.
     */
    fun notifyRisk(level: RiskLevel, category: String, detectedPhrase: String? = null, recordId: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAV_ROUTE, routeFor(level, recordId))
        }
        val pending = PendingIntent.getActivity(
            context,
            // 기록마다 요청 코드를 달리해, 이전 알림의 이동 경로가 새 알림에 섞이지 않게 한다.
            recordId?.hashCode() ?: level.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 사용자가 중립 문구를 켜 두었으면 위험도·감지 내용을 드러내지 않는 문구로 대체한다
        // (가해자와 화면을 공유하는 상황 대비 — Design.md 7장)
        if (NotificationTextStore.isCustomEnabled(context)) {
            val neutral = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_warning)
                .setContentTitle(NotificationTextStore.title(context))
                .setContentText(NotificationTextStore.body(context))
                .setPriority(priorityOf(level))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            runCatching { manager().notify(NOTIF_ID, neutral) }
            return
        }

        val contentText = detectedPhrase
            ?.takeIf { it.isNotBlank() }
            ?.let { "감지된 표현: \"$it\"" }
            ?: "확인이 필요한 표현이 감지되었습니다. 내용을 확인해 보세요."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // 앱 아이콘 대신 경고 삼각형(느낌표) — 상태바/알림에서 "경고"임이 바로 보이도록
            .setSmallIcon(R.drawable.ic_stat_warning)
            // 위험도별 강조색: 알림 서랍에서 아이콘 배경이 빨강(치명)·주황(경고)으로 물듦
            .setColor(accentColorOf(level))
            .setContentTitle(neutralTitle(level, category))
            .setContentText(contentText)
            .setPriority(priorityOf(level))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching { manager().notify(NOTIF_ID, notification) }
    }

    private fun manager(): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * 알림을 눌렀을 때 열 화면.
     *
     * 기록이 있으면 그 기록의 분석 결과 화면으로 간다. 예전에는 긴급이면 긴급 도움, 그 외엔 대응 가이드로
     * 바로 보내서, 사용자가 무엇이 왜 감지됐는지 확인할 방법이 없었다. 결과 화면에서 긴급 도움·대응
     * 가이드로 이어지므로(긴급이면 "긴급 도움 요청"이 주 버튼) 한 번만 더 누르면 같은 곳에 닿는다.
     * 기록 저장에 실패한 경우에만 예전처럼 위험도별 화면으로 보낸다.
     */
    private fun routeFor(level: RiskLevel, recordId: String?): String = when {
        recordId != null -> Screen.DetectionResult.createRoute(recordId)
        level == RiskLevel.CRITICAL -> Screen.Emergency.route
        else -> Screen.ResponseGuide.createRoute(level)
    }

    private fun neutralTitle(level: RiskLevel, category: String): String = when (level) {
        RiskLevel.CRITICAL -> "지금 바로 확인해 보세요"
        RiskLevel.WARNING -> "확인이 필요한 표현이 감지되었어요"
        else -> if (category.isBlank()) "SafeLink 알림" else "$category 관련 표현이 감지되었어요"
    }

    /** 알림 강조색 — 치명(빨강)·경고(주황)·그 외(브랜드 블루). ARGB. */
    private fun accentColorOf(level: RiskLevel): Int = when (level) {
        RiskLevel.CRITICAL -> 0xFFD32F2F.toInt() // 경고 빨강
        RiskLevel.WARNING -> 0xFFF57C00.toInt()  // 주의 주황
        else -> 0xFF1E347A.toInt()               // 브랜드 블루
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
