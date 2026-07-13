package com.safelink.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite

/** 전역 SOS 플로팅 버튼 — 긴급 도움 요청 화면으로 이동 */
@Composable
fun SosFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = RiskCritical,
        contentColor = SurfaceWhite,
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Filled.Sos, contentDescription = "긴급 도움 요청")
    }
}
