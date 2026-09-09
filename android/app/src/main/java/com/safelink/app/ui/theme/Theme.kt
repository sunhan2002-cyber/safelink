package com.safelink.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandBlueDark,
    onSecondary = SurfaceWhite,
    // secondaryContainer를 명시 안 해서 Material3 기본(보라 계열)이 하단 네비게이션 선택
    // 표시·세그먼트 토글 선택 배경에 그대로 새어나오던 문제 - 브랜드 블루 계열로 채움
    // (UI/UX 리뷰 중 발견, 전체 화면 공통 컴포넌트라 여기 한 곳만 고치면 전부 적용됨)
    secondaryContainer = BrandBlueLight,
    onSecondaryContainer = BrandBlueDark,
    error = RiskCritical,
    onError = SurfaceWhite,
    errorContainer = RiskCriticalContainer,
    background = BackgroundGray,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun SafeLinkTheme(content: @Composable () -> Unit) {
    // 안전 서비스 특성상 라이트 테마 고정 (다크 모드는 추후 팀 결정)
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
