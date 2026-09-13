package com.safelink.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** 흰색 라운드 카드 — 모든 화면 공용. containerColor로 강조 카드(연파랑 등) 지원 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SafeLinkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    helpTitle: String? = null,
    helpDescription: String? = null,
    containerColor: Color? = null,
    containerBrush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val baseContainerColor = if (containerBrush != null) Color.Transparent
    else containerColor ?: MaterialTheme.colorScheme.surface
    val contentBackground = if (containerBrush != null) {
        Modifier.background(containerBrush)
    } else {
        Modifier
    }
    // 배경(BackgroundGray)과 카드가 거의 붙어 보이던 문제 대응 — 은은한 그림자로 뜨는 느낌만 추가
    // (너무 진하면 산만해지니 낮은 값 유지, 눌렀을 때는 살짝 더)
    val elevation = CardDefaults.cardElevation(
        defaultElevation = 2.dp,
        pressedElevation = 1.dp
    )
    if (onClick != null && helpTitle != null && helpDescription != null) {
        var showDialog by remember { mutableStateOf(false) }
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val hovered by interactionSource.collectIsHoveredAsState()
        val scale by animateFloatAsState(
            targetValue = when {
                pressed -> 0.98f
                hovered -> 1.02f
                else -> 1f
            },
            label = "card-feedback"
        )
        val activeContainerColor by animateColorAsState(
            targetValue = if (hovered && containerColor == null) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                baseContainerColor
            },
            label = "card-hover-color"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onLongClickLabel = "$helpTitle 설명 보기",
                        onLongClick = {
                            showDialog = true
                        },
                        onClick = onClick
                    ),
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = activeContainerColor),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (hovered) 7.dp else if (pressed) 1.dp else 2.dp
                ),
                border = if (hovered) BorderStroke(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                ) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(contentBackground)
                        .padding(16.dp),
                    content = content
                )
            }
            ActionHelpTooltip(hovered && !showDialog, helpTitle, helpDescription)
        }
        ActionHelpDialog(showDialog, helpTitle, helpDescription) { showDialog = false }
    } else if (onClick != null) {
        val colors = CardDefaults.cardColors(containerColor = baseContainerColor)
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(contentBackground)
                    .padding(16.dp),
                content = content
            )
        }
    } else {
        val colors = CardDefaults.cardColors(containerColor = baseContainerColor)
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(contentBackground)
                    .padding(16.dp),
                content = content
            )
        }
    }
}
