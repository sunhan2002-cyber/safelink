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
