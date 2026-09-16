import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/** local.properties — 이 PC 전용 설정. 저장소에 커밋되지 않는다(.gitignore 대상). */
val localProperties: Properties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

/**
 * Safe Browsing API 키는 저장소에 커밋하지 않는다(local.properties, .gitignore 대상).
 * 키가 없으면 빈 문자열이 들어가고, 링크 검사 기능만 "사용 불가"로 표시된 채 앱은 정상 동작한다.
 */
val safeBrowsingApiKey: String = localProperties.getProperty("SAFE_BROWSING_API_KEY", "")

/**
 * AI 문맥 분석 서버 주소.
 *
 * 기본값 10.0.2.2 는 에뮬레이터 안에서만 호스트 PC 를 가리키는 특수 주소다. 실기기에서는 서버에
 * 절대 닿지 않고, 그때 앱은 조용히 온디바이스 결과만 쓴다. 실기기나 배포 서버에 붙이려면
 * local.properties 에 SAFELINK_AI_BASE_URL=https://... 를 넣고 다시 빌드한다.
 * Retrofit 은 주소 끝에 / 가 있어야 하므로 없으면 붙인다.
 */
val aiBaseUrl: String = localProperties.getProperty("SAFELINK_AI_BASE_URL", "http://10.0.2.2:8000/")
    .trim()
    .let { if (it.endsWith("/")) it else "$it/" }

/**
 * Claude API 키. 발표용으로 앱이 Claude 를 직접 호출한다(서버를 거치지 않음).
 * local.properties 에만 둔다(.gitignore 대상). 키가 APK 안에 들어가므로 APK 파일을 외부에 공유하지 않는다.
 * 키가 없으면 기존처럼 분석 서버(SAFELINK_AI_BASE_URL)로 요청한다.
 */
val anthropicApiKey: String = localProperties.getProperty("ANTHROPIC_API_KEY", "")

android {
    namespace = "com.safelink.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.safelink.app"
        minSdk = 26
        targetSdk = 35
        // 설정 화면 맨 아래 "SafeLink vX.Y.Z"와 공유하는 APK 파일 이름(SafeLink_vX.Y.Z_...apk)을 같은 번호로 맞춘다.
        // versionCode 는 X*100 + Y*10 + Z (0.5.0 → 50). 새 APK 를 나눠줄 때마다 올려야 덮어 설치가 된다.
        versionCode = 61
        versionName = "0.6.1"

        buildConfigField("String", "SAFE_BROWSING_API_KEY", "\"$safeBrowsingApiKey\"")
        buildConfigField("String", "AI_BASE_URL", "\"$aiBaseUrl\"")
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"$anthropicApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 기기 테스트(PipelineDeviceTest)는 JVM 회귀 테스트와 같은 문장 묶음을 쓴다
    sourceSets["androidTest"].assets.srcDirs("src/test/resources")

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
    // AI 문맥 분석 — 발표용으로 앱이 Claude 를 직접 호출 (공식 Java SDK, Kotlin 에서 사용)
    implementation(libs.anthropic.java)
    testImplementation(libs.junit)
    // 실제 기기(에뮬레이터)에서 분석 경로 전체를 확인하는 테스트 — 안드로이드 정규식·ML Kit 인식까지 포함
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
