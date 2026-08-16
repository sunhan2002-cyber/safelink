package com.safelink.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 화면의 액션 버튼(전화·홈페이지·문자)을 실제 시스템 인텐트로 실행한다.
 *
 * - 전화: ACTION_DIAL — 다이얼러에 번호만 채워 열어준다(자동 발신 아님). CALL_PHONE 권한 불필요.
 * - 홈페이지: ACTION_VIEW — 기본 브라우저로 연다.
 * - 문자: ACTION_SENDTO(smsto:) — 문자 앱에 수신번호+본문을 채워 열어준다(자동 전송 아님).
 *
 * 실패(대상 앱 없음 등)해도 앱이 죽지 않도록 runCatching 으로 감싼다.
 */
object IntentActions {

    fun dial(context: Context, phoneNumber: String) {
        val number = phoneNumber.filter { it.isDigit() || it == '+' }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        }
    }

    fun openWeb(context: Context, url: String) {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
        }
    }

    fun sendSms(context: Context, phoneNumber: String, body: String) {
        val number = phoneNumber.filter { it.isDigit() || it == '+' }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                    .putExtra("sms_body", body)
            )
        }
    }
}
