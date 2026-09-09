import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Safe Browsing API 키는 저장소에 커밋하지 않는다(local.properties, .gitignore 대상).
 * 키가 없으면 빈 문자열이 들어가고, 링크 검사 기능만 "사용 불가"로 표시된 채 앱은 정상 동작한다.
 */
val safeBrowsingApiKey: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("SAFE_BROWSING_API_KEY", "")

android {
    namespace = "com.safelink.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.safelink.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SAFE_BROWSING_API_KEY", "\"$safeBrowsingApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.gson)

    // 생체인증 잠금 해제 (Task 5.13)
    implementation(libs.androidx.biometric)

    // 검사 기록 로컬 저장 (Task 7.1) — 기기 내에만 저장, 서버 전송 없음
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // 스크린샷 분석용 온디바이스 OCR (한국어 텍스트 인식, 오프라인)
    implementation(libs.mlkit.text.recognition.korean)
    // 2차 AI 보조 분석 (네트워크)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    // 링크 안전성 검사 — Google Play 서비스가 관리하는 온디바이스 차단 목록 조회(URL 미전송)
    implementation(libs.play.services.safebrowsing)
    testImplementation(libs.junit)
}
