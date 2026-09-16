package com.safelink.app

import android.app.Application
import com.safelink.app.data.remote.ClaudeDirectAnalyzer
import com.safelink.app.settings.FeatureToggleState
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SafeLinkApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 스크린샷 공유 입구를 켜 둔다 (예전 "스크린샷 분석 사용" 스위치로 꺼 둔 사용자 정리)
        FeatureToggleState.load(this)
        // 발표용 구성: 첫 AI 분석이 SDK 준비 때문에 느려지지 않도록 앱이 켜질 때 미리 준비한다.
        // 화면 표시를 막지 않게 별도 스레드에서 돌린다. 키가 없으면 아무것도 하지 않는다.
        Thread({ ClaudeDirectAnalyzer.warmUp() }, "claude-warmup").start()
    }
}
