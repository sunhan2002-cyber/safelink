package com.safelink.app

import android.app.Application
import com.safelink.app.data.remote.ClaudeDirectAnalyzer
import com.safelink.app.settings.FeatureToggleState
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SafeLinkApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 저장된 기능 토글을 메모리 상태로 올린다 (설정에서 끈 값이 재시작 후에도 유지되도록)
        FeatureToggleState.load(this)
        // 발표용 구성: 첫 AI 분석이 SDK 준비 때문에 느려지지 않도록 앱이 켜질 때 미리 준비한다.
        // 화면 표시를 막지 않게 별도 스레드에서 돌린다. 키가 없으면 아무것도 하지 않는다.
        Thread({ ClaudeDirectAnalyzer.warmUp() }, "claude-warmup").start()
    }
}
