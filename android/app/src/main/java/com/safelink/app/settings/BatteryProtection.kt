package com.safelink.app.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * 휴대폰 절전 기능이 SafeLink(실시간 보호)를 멈추지 못하게 하는 설정.
 *
 * ── 왜 필요한가 ────────────────────────────────────────────────────────
 * 삼성 기기는 한동안 안 쓴 앱을 얼려 두는데(앱 절전), 이때 접근성 권한은 켜진 채로 화면 변화 신호만 끊긴다.
 * 홈에는 "실시간 보호 중"이라고 보이는데 사기 메시지를 놓치는 상태가 된다. 실기기(갤럭시 S23 FE)에서
 * 인스타 DM 이 감지되지 않던 원인이었고, 앱 배터리 설정을 "제한 없음"으로 바꾸자 해결됐다.
 * 사용자는 이 설정이 있는지 모르고, 멈춰도 조용히 안 잡힐 뿐이라 스스로 알아챌 방법이 없다.
 *
 * ── 무엇으로 판단하나 ──────────────────────────────────────────────────
 * [PowerManager.isIgnoringBatteryOptimizations] — 삼성의 앱 배터리 "제한 없음"을 고르면 true 가 된다.
 * 단, "디바이스 케어 > 백그라운드 사용 한도"의 "딥 절전 앱" 목록에 사용자가 직접 넣은 경우는 앱이 알 수 없다
 * (안내 창에 한 줄 적어 둔다).
 */
object BatteryProtection {

    fun isUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** 삼성 기기면 "딥 절전 앱" 안내를 덧붙인다. */
    val isSamsung: Boolean get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    /**
     * "배터리 제한 해제" 시스템 창을 띄운다(사용자는 [허용] 한 번). 이 창을 띄울 수 없는 기종이면
     * 앱 정보 화면으로 보내 사용자가 배터리 → 제한 없음을 고르게 한다.
     *
     * 이 권한(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)은 구글 플레이 스토어 심사에서 용도가 제한된다.
     * 지금은 APK 직접 설치로 배포하므로 문제없지만, 스토어에 올릴 때는 앱 정보 화면 안내만 남겨야 할 수 있다.
     */
    @SuppressLint("BatteryLife")
    fun requestUnrestricted(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        val opened = runCatching { context.startActivity(direct) }.isSuccess
        if (!opened) openAppDetails(context)
    }

    fun openAppDetails(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    const val DIALOG_TITLE = "휴대폰이 SafeLink를 멈추지 않게 해 주세요"

    const val DIALOG_BODY =
        "휴대폰이 배터리를 아끼려고 SafeLink를 멈추면, 그동안 온 사기 메시지를 알려드릴 수 없어요.\n\n" +
            "아래 [설정하기]를 누르고, 뜨는 창에서 '허용'을 눌러 주세요."

    const val SAMSUNG_NOTE =
        "삼성 휴대폰: 설정 > 디바이스 케어(배터리) > 백그라운드 사용 한도에서 SafeLink가 '딥 절전 앱'에 들어 있으면 빼 주세요."
}
