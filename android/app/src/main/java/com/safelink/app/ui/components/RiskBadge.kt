package com.safelink.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.theme.RiskCaution
import com.safelink.app.ui.theme.RiskCautionContainer
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.RiskCriticalContainer
import com.safelink.app.ui.theme.RiskSafe
import com.safelink.app.ui.theme.RiskSafeContainer
import com.safelink.app.ui.theme.RiskWarning
import com.safelink.app.ui.theme.RiskWarningContainer

fun RiskLevel.color(): Color = when (this) {
    RiskLevel.SAFE -> RiskSafe
    RiskLevel.CAUTION -> RiskCaution
    RiskLevel.WARNING -> RiskWarning
    RiskLevel.CRITICAL -> RiskCritical
}

fun RiskLevel.containerColor(): Color = when (this) {
    RiskLevel.SAFE -> RiskSafeContainer
    RiskLevel.CAUTION -> RiskCautionContainer
    RiskLevel.WARNING -> RiskWarningContainer
    RiskLevel.CRITICAL -> RiskCriticalContainer
}

/** 위험도 배지 — 기록 카드, 결과 화면 등에서 공용 사용 */
@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    Text(
        text = level.label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = level.color(),
        modifier = modifier
            .background(level.containerColor(), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
