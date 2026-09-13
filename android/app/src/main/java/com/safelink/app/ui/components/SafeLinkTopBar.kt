package com.safelink.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** 공통 상단바 — 뒤로가기/닫기 + 타이틀 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeLinkTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    useCloseIcon: Boolean = false,
    collapseFraction: Float = 0f,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val fraction = collapseFraction.coerceIn(0f, 1f)
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .blur((fraction * 8f).dp)
                    .graphicsLayer { alpha = 1f - fraction }
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = if (useCloseIcon) Icons.Filled.Close
                        else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (useCloseIcon) "닫기" else "뒤로 가기"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
