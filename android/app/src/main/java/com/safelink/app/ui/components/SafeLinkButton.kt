package com.safelink.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
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
    leadingIcon: ImageVector? = null,
    helpTitle: String? = null,
    helpDescription: String? = null
) {
    if (helpTitle != null && helpDescription != null) {
        HelpAwarePrimaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            containerColor = containerColor,
            height = height,
            textStyle = textStyle,
            leadingIcon = leadingIcon,
            helpTitle = helpTitle,
            helpDescription = helpDescription
        )
        return
    }

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

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HelpAwarePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    containerColor: Color,
    height: Dp,
    textStyle: TextStyle?,
    leadingIcon: ImageVector?,
    helpTitle: String,
    helpDescription: String
) {
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.98f
            hovered -> 1.015f
            else -> 1f
        },
        label = "button-feedback"
    )
    val activeColor by animateColorAsState(
        targetValue = if (hovered) MaterialTheme.colorScheme.secondary else containerColor,
        label = "button-hover-color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource, enabled = enabled)
    ) {
        Surface(
            color = if (enabled) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = when {
                pressed -> 1.dp
                hovered -> 7.dp
                else -> 2.dp
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .scale(scale)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onLongClickLabel = "$helpTitle 설명 보기",
                    onLongClick = {
                        showDialog = true
                    },
                    onClick = onClick
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = text, style = textStyle ?: MaterialTheme.typography.labelLarge)
            }
        }
        ActionHelpTooltip(hovered && !showDialog, helpTitle, helpDescription)
    }

    ActionHelpDialog(
        visible = showDialog,
        title = helpTitle,
        description = helpDescription,
        onDismiss = { showDialog = false }
    )
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
