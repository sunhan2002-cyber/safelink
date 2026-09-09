package com.safelink.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 생체인증(지문·얼굴) 잠금 해제 (Task 5.13).
 *
 * PIN 을 대체하는 것이 아니라 **빠른 해제 수단**으로만 쓴다. 생체인증에 실패하거나 기기가
 * 지원하지 않으면 기존 PIN 입력 흐름이 그대로 남아 있어 잠금이 풀리지 않는 상황은 없다.
 *
 * 지문 정보 자체는 앱이 들여다볼 수 없고(Android 시스템이 처리) 성공/실패 결과만 전달받는다.
 */
object BiometricAuth {

    /** 이 기기에서 생체인증을 실제로 쓸 수 있는지(센서 존재 + 등록된 생체정보 있음) */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * 생체인증 창을 띄운다.
     *
     * @param onSuccess 인증 성공 — 호출부에서 홈으로 이동시키면 된다
     * @param onFailed  사용자가 취소했거나 인증이 불가능한 경우. PIN 입력으로 계속 진행하면 된다.
     */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailed: (message: String?) -> Unit = {}
    ) {
        if (!isAvailable(activity)) {
            onFailed("이 기기에서는 생체인증을 사용할 수 없습니다. PIN을 입력해 주세요.")
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 사용자가 직접 취소한 경우는 안내 문구 없이 조용히 PIN 입력으로 돌아간다
                    val userCancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    onFailed(if (userCancelled) null else errString.toString())
                }
            }
        )

        // 알림과 마찬가지로 앱 목적이 드러나지 않는 중립적 문구를 쓴다(Design.md 7장)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("잠금 해제")
            .setSubtitle("등록된 지문 또는 얼굴로 잠금을 해제합니다")
            .setNegativeButtonText("PIN 입력")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(info)
    }
}
