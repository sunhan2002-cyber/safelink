package com.safelink.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Primary 대형 버튼 — 접근성 위해 56dp 높이가 기본값.
 * [height]/[textStyle]을 선택적으로 받아 특정 화면(예: 홈의 핵심 CTA)에서만 더 크게 쓸 수
 * 있게 함 — 컴포넌트 기본값 자체를 바꾸면 이 버튼을 쓰는 다른 10곳 화면까지 전부 커지므로 주의.
 * [leadingIcon]은 기본 null(기존 화면 전부 영향 없음) — 전화 연결처럼 "이 버튼이 뭘 하는지"를
 * 아이콘으로 보강하고 싶은 곳에서만 지정 (Figma 리디자인 시안 검토 중 발견 - 큰 숫자/텍스트는
 * 그대로 두고 옆에 작은 아이콘만 더하는 게 접근성엔 로고 교체보다 안전하다는 판단).
 */
@Composable
fun SafeLinkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 56.dp,
    textStyle: TextStyle? = null,
    leadingIcon: ImageVector? = null
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
        if (leadingIcon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, style = textStyle ?: MaterialTheme.typography.labelLarge)
            }
        } else {
            Text(text = text, style = textStyle ?: MaterialTheme.typography.labelLarge)
        }
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
