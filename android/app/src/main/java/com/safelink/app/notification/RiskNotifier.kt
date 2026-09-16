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
        // 위험도별로 채널을 나눈다 (Design.md 5.3: 주의=무음, 경고=진동, 긴급=진동+소리).
        // 안드로이드 8 이상에서는 소리·진동을 채널이 정하고 setPriority 는 무시되므로, 채널 하나로는
        // 어떤 우선순위를 줘도 전부 같은 소리로 울렸다.
        // 채널 이름은 시스템 설정에도 그대로 보이므로 위험·폭력 같은 민감한 단어를 넣지 않는다(Design.md 7장).
        CHANNELS.forEach { (id, spec) ->
            val channel = NotificationChannel(id, spec.displayName, spec.importance)
            manager().createNotificationChannel(channel)
        }
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
            val neutral = NotificationCompat.Builder(context, channelIdOf(level))
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

        val notification = NotificationCompat.Builder(context, channelIdOf(level))
            // 앱 아이콘 대신 경고 삼각형(느낌표) — 상태바/알림에서 "경고"임이 바로 보이도록
            .setSmallIcon(R.drawable.ic_stat_warning)
            // 위험도별 마크 색: 알림의 경고 아이콘 원이 빨강(경고 이상)·주황(주의 이하)으로 물듦
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

    /**
     * 알림 경고 마크(아이콘 원) 색 — 경고 이상은 빨강, 주의 이하는 주황. ARGB.
     * 본문 글자는 시스템 기본 모양 그대로 두고 마크 색으로만 위험을 알린다.
     */
    private fun accentColorOf(level: RiskLevel): Int = when (level) {
        RiskLevel.CRITICAL, RiskLevel.WARNING -> 0xFFD32F2F.toInt() // 빨강
        else -> 0xFFF57C00.toInt()                                  // 주황
    }

    private fun priorityOf(level: RiskLevel): Int = when (level) {
        RiskLevel.CRITICAL -> NotificationCompat.PRIORITY_HIGH
        RiskLevel.WARNING -> NotificationCompat.PRIORITY_DEFAULT
        else -> NotificationCompat.PRIORITY_LOW
    }

    private fun channelIdOf(level: RiskLevel): String = when (level) {
        RiskLevel.CRITICAL -> CHANNEL_CRITICAL
        RiskLevel.WARNING -> CHANNEL_WARNING
        else -> CHANNEL_LOW
    }

    private data class ChannelSpec(val displayName: String, val importance: Int)

    companion object {
        /** 긴급 — 진동 + 알림음 */
        const val CHANNEL_CRITICAL = "safelink_alert"

        /** 경고 — 진동 */
        const val CHANNEL_WARNING = "safelink_alert_warning"

        /** 주의 이하 — 무음 */
        const val CHANNEL_LOW = "safelink_alert_low"

        private val CHANNELS = mapOf(
            // 채널 이름은 시스템 설정(앱 알림 설정)에 그대로 노출된다. 가해자와 화면을 함께 보는 상황을
            // 감안해 "긴급" 같은 단어 대신 중요도만 밝힌다(Design.md 7장).
            CHANNEL_CRITICAL to ChannelSpec("중요도 높음", NotificationManager.IMPORTANCE_HIGH),
            CHANNEL_WARNING to ChannelSpec("중요도 보통", NotificationManager.IMPORTANCE_DEFAULT),
            CHANNEL_LOW to ChannelSpec("중요도 낮음", NotificationManager.IMPORTANCE_LOW)
        )

        private const val NOTIF_ID = 1001
    }
}
