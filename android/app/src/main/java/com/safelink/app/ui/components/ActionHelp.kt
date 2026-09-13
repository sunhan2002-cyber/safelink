package com.safelink.app.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/** 마우스를 올렸을 때 작업을 방해하지 않고 버튼 가까이에 보이는 짧은 설명. */
@Composable
internal fun ActionHelpTooltip(
    visible: Boolean,
    title: String,
    description: String
) {
    if (!visible) return

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, -12),
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** 길게 눌렀을 때 화면 중앙에 표시하는 기능 설명. 확인 전에는 실제 작업을 실행하지 않는다. */
@Composable
internal fun ActionHelpDialog(
    visible: Boolean,
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            EnableBlurBehindDialog()
            Text(text = "${title}은 이런 작업을 해요")
        },
        text = { Text(text = description, style = MaterialTheme.typography.bodyLarge) },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

/**
 * 기본 검정 dim 대신 뒤 화면을 흐리게 한다. Android 12 미만이나 기기에서 시스템 블러를
 * 비활성화한 경우에는 옅은 dim만 남겨 설명창과 본문의 대비를 유지한다.
 */
@Composable
internal fun EnableBlurBehindDialog() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.10f else 0.18f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { blurBehindRadius = 48 }
            }
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }
}
