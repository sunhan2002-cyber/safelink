package com.safelink.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Primary 대형 버튼 — 접근성 위해 56dp 높이가 기본값.
 * [height]/[textStyle]을 선택적으로 받아 특정 화면(예: 홈의 핵심 CTA)에서만 더 크게 쓸 수
 * 있게 함 — 컴포넌트 기본값 자체를 바꾸면 이 버튼을 쓰는 다른 10곳 화면까지 전부 커지므로 주의.
 */
@Composable
fun SafeLinkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 56.dp,
    textStyle: TextStyle? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Text(text = text, style = textStyle ?: MaterialTheme.typography.labelLarge)
    }
}

/** Outlined 보조 버튼 */
@Composable
fun SafeLinkOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
