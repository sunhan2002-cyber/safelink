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

    fun notifyRisk(level: RiskLevel, category: String, detectedPhrase: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAV_ROUTE, routeFor(level))
        }
        val pending = PendingIntent.getActivity(
            context,
            level.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = detectedPhrase
            ?.takeIf { it.isNotBlank() }
            ?.let { "감지된 표현: \"$it\"" }
            ?: "확인이 필요한 표현이 감지되었습니다. 내용을 확인해 보세요."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(neutralTitle(level, category))
            .setContentText(contentText)
            .setPriority(priorityOf(level))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching { manager().notify(NOTIF_ID, notification) }
    }

    private fun manager(): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun routeFor(level: RiskLevel): String = when (level) {
        RiskLevel.CRITICAL -> Screen.Emergency.route
        else -> Screen.ResponseGuide.createRoute(level)
    }

    private fun neutralTitle(level: RiskLevel, category: String): String = when (level) {
        RiskLevel.CRITICAL -> "지금 바로 확인해 보세요"
        RiskLevel.WARNING -> "확인이 필요한 표현이 감지되었어요"
        else -> if (category.isBlank()) "SafeLink 알림" else "$category 관련 표현이 감지되었어요"
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
